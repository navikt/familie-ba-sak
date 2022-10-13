package no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakBehandlingService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.SmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.beregning.VedtaksperiodefinnerSmåbarnstilleggFeil
import no.nav.familie.ba.sak.kjerne.beregning.finnAktuellVedtaksperiodeOgLeggTilSmåbarnstilleggbegrunnelse
import no.nav.familie.ba.sak.kjerne.beregning.hentInnvilgedeOgReduserteAndelerSmåbarnstillegg
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.RuntimeException

@Service
class AutovedtakSmåbarnstilleggService(
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val vedtakService: VedtakService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val småbarnstilleggService: SmåbarnstilleggService,
    private val taskRepository: TaskRepository,
    private val beregningService: BeregningService,
    private val autovedtakService: AutovedtakService,
    private val oppgaveService: OppgaveService,
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService
) : AutovedtakBehandlingService<Aktør> {

    private val antallVedtakOmOvergangsstønad: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "antall")
    private val antallVedtakOmOvergangsstønadPåvirkerFagsak: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "paavirker_fagsak")
    private val antallVedtakOmOvergangsstønadPåvirkerIkkeFagsak: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "paavirker_ikke_fagsak")

    enum class TilManuellBehandlingÅrsak(val beskrivelse: String) {
        NYE_UTBETALINGSPERIODER_FØRER_TIL_MANUELL_BEHANDLING("Endring i OS gir etterbetaling, feilutbetaling eller endring mer enn 1 måned frem i tid"),
        KLARER_IKKE_BEGRUNNE("Klarer ikke å begrunne")
    }

    private val antallVedtakOmOvergangsstønadTilManuellBehandling: Map<TilManuellBehandlingÅrsak, Counter> =
        TilManuellBehandlingÅrsak.values().associateWith {
            Metrics.counter(
                "behandling",
                "saksbehandling",
                "hendelse",
                "smaabarnstillegg",
                "til_manuell_behandling",
                "aarsak",
                it.name,
                "beskrivelse",
                it.beskrivelse
            )
        }

    override fun skalAutovedtakBehandles(behandlingsdata: Aktør): Boolean {
        val fagsak = fagsakService.hent(aktør = behandlingsdata) ?: return false
        val påvirkerFagsak = småbarnstilleggService.vedtakOmOvergangsstønadPåvirkerFagsak(fagsak)
        return if (!påvirkerFagsak) {
            antallVedtakOmOvergangsstønadPåvirkerIkkeFagsak.increment()

            logger.info("Påvirker ikke fagsak")
            false
        } else {
            antallVedtakOmOvergangsstønadPåvirkerFagsak.increment()
            true
        }
    }

    @Transactional
    override fun kjørBehandling(aktør: Aktør): String {
        antallVedtakOmOvergangsstønad.increment()
        val fagsak = fagsakService.hent(aktør, FagsakType.NORMAL)
            ?: throw RuntimeException("Skal ikke kunne skje. Sjekk på om fagsak eksisterer er gjort tidligare i call-stacken")
        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                aktør = aktør,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.SMÅBARNSTILLEGG,
                fagsakId = fagsak.id
            )

        if (behandlingEtterBehandlingsresultat.status != BehandlingStatus.IVERKSETTER_VEDTAK) {
            return kanIkkeBehandleAutomatisk(
                behandling = behandlingEtterBehandlingsresultat,
                metric = antallVedtakOmOvergangsstønadTilManuellBehandling[TilManuellBehandlingÅrsak.NYE_UTBETALINGSPERIODER_FØRER_TIL_MANUELL_BEHANDLING]!!,
                meldingIOppgave = "Småbarnstillegg: endring i overgangsstønad må behandles manuelt"
            )
        }

        try {
            begrunnAutovedtakForSmåbarnstillegg(behandlingEtterBehandlingsresultat)
        } catch (e: VedtaksperiodefinnerSmåbarnstilleggFeil) {
            logger.warn(e.message, e)
            return kanIkkeBehandleAutomatisk(
                behandling = behandlingEtterBehandlingsresultat,
                metric = antallVedtakOmOvergangsstønadTilManuellBehandling[TilManuellBehandlingÅrsak.KLARER_IKKE_BEGRUNNE]!!,
                meldingIOppgave = "Småbarnstillegg: klarer ikke bestemme vedtaksperiode som skal begrunnes, må behandles manuelt"
            )
        }

        val vedtakEtterTotrinn = autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
            behandlingEtterBehandlingsresultat
        )

        val task = IverksettMotOppdragTask.opprettTask(
            behandlingEtterBehandlingsresultat,
            vedtakEtterTotrinn,
            SikkerhetContext.hentSaksbehandler()
        )
        taskRepository.save(task)

        return AutovedtakStegService.BEHANDLING_FERDIG
    }

    private fun begrunnAutovedtakForSmåbarnstillegg(
        behandlingEtterBehandlingsresultat: Behandling
    ) {
        val sistIverksatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = behandlingEtterBehandlingsresultat.fagsak.id)
        val forrigeSmåbarnstilleggAndeler =
            if (sistIverksatteBehandling == null) {
                emptyList()
            } else {
                beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(
                    behandlingId = sistIverksatteBehandling.id
                ).filter { it.erSmåbarnstillegg() }
            }

        val nyeSmåbarnstilleggAndeler =
            if (sistIverksatteBehandling == null) {
                emptyList()
            } else {
                beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(
                    behandlingId = behandlingEtterBehandlingsresultat.id
                ).filter { it.erSmåbarnstillegg() }
            }

        val (innvilgedeMånedPerioder, reduserteMånedPerioder) = hentInnvilgedeOgReduserteAndelerSmåbarnstillegg(
            forrigeSmåbarnstilleggAndeler = forrigeSmåbarnstilleggAndeler,
            nyeSmåbarnstilleggAndeler = nyeSmåbarnstilleggAndeler
        )

        vedtaksperiodeHentOgPersisterService.lagre(
            finnAktuellVedtaksperiodeOgLeggTilSmåbarnstilleggbegrunnelse(
                innvilgetMånedPeriode = innvilgedeMånedPerioder.singleOrNull(),
                redusertMånedPeriode = reduserteMånedPerioder.singleOrNull(),
                vedtaksperioderMedBegrunnelser = vedtaksperiodeService.hentPersisterteVedtaksperioder(
                    vedtak = vedtakService.hentAktivForBehandlingThrows(
                        behandlingId = behandlingEtterBehandlingsresultat.id
                    )
                )
            )
        )
    }

    private fun kanIkkeBehandleAutomatisk(
        behandling: Behandling,
        metric: Counter,
        meldingIOppgave: String
    ): String {
        metric.increment()
        val omgjortBehandling = autovedtakService.omgjørBehandlingTilManuellOgKjørSteg(
            behandling = behandling,
            steg = StegType.VILKÅRSVURDERING
        )
        return oppgaveService.opprettOppgaveForManuellBehandling(
            behandling = omgjortBehandling,
            begrunnelse = meldingIOppgave,
            oppgavetype = Oppgavetype.VurderLivshendelse,
            opprettLogginnslag = true
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakSmåbarnstilleggService::class.java)
    }
}
