package no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.erÅpen
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.SmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.beregning.VedtaksperiodefinnerSmåbarnstilleggFeil
import no.nav.familie.ba.sak.kjerne.beregning.finnAktuellVedtaksperiodeOgLeggTilSmåbarnstilleggbegrunnelse
import no.nav.familie.ba.sak.kjerne.beregning.hentInnvilgedeOgReduserteAndelerSmåbarnstillegg
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VedtakOmOvergangsstønadService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val småbarnstilleggService: SmåbarnstilleggService,
    private val taskRepository: TaskRepository,
    private val featureToggleService: FeatureToggleService,
    private val beregningService: BeregningService,
    private val autovedtakService: AutovedtakService
) {

    private val antallVedtakOmOvergangsstønad: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "antall")
    private val antallVedtakOmOvergangsstønadÅpenBehandling: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "aapen_behandling")
    private val antallVedtakOmOvergangsstønadPåvirkerFagsak: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "paavirker_fagsak")
    private val antallVedtakOmOvergangsstønadPåvirkerIkkeFagsak: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "paavirker_ikke_fagsak")

    enum class TilManuellBehandlingÅrsak(val beskrivelse: String) {
        ETTERBETALING_ELLER_FEILUTBETALING("Endring i OS gir etterbetaling eller feilutbetaling"),
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
                it.beskrivelse,
            )
        }

    @Transactional
    fun håndterVedtakOmOvergangsstønad(personIdent: String): String {
        if (!featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_SMÅBARNSTILLEGG_AUTOMATISK))
            return "automatisk behandling av småbarnstillegg er ikke påskrudd"

        antallVedtakOmOvergangsstønad.increment()

        val fagsak = fagsakService.hent(personIdent = PersonIdent(personIdent)) ?: return "har ikke fagsak i systemet"
        val aktivBehandling = behandlingService.hentAktivForFagsak(fagsakId = fagsak.id)

        if (aktivBehandling == null) {
            return "har ikke vært noe behandling på fagsak, ingenting å revurdere"
        } else if (aktivBehandling.status.erÅpen()) {
            antallVedtakOmOvergangsstønadÅpenBehandling.increment()
            return autovedtakService.opprettOppgaveForManuellBehandling(
                behandling = aktivBehandling,
                begrunnelse = "Behandling av småbarnstillegg stoppet pga. ", // TODO
                oppgavetype = Oppgavetype.VurderLivshendelse,
                opprettLogginnslag = false
            )
        }

        val påvirkerFagsak = småbarnstilleggService.vedtakOmOvergangsstønadPåvirkerFagsak(fagsak)

        return if (påvirkerFagsak) {
            antallVedtakOmOvergangsstønadPåvirkerFagsak.increment()

            val behandlingEtterBehandlingsresultat =
                autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                    fagsak = fagsak,
                    behandlingType = BehandlingType.REVURDERING,
                    behandlingÅrsak = BehandlingÅrsak.SMÅBARNSTILLEGG
                )

            if (behandlingEtterBehandlingsresultat.status != BehandlingStatus.IVERKSETTER_VEDTAK) {
                return kanIkkeBehandleAutomatisk(
                    behandling = behandlingEtterBehandlingsresultat,
                    metric = antallVedtakOmOvergangsstønadTilManuellBehandling[TilManuellBehandlingÅrsak.ETTERBETALING_ELLER_FEILUTBETALING]!!,
                    meldingIOppgave = "" // TODO
                )
            }

            try {
                begrunnAutovedtakForSmåbarnstillegg(behandlingEtterBehandlingsresultat)
            } catch (vedtaksperiodefinnerSmåbarnstilleggFeil: VedtaksperiodefinnerSmåbarnstilleggFeil) {
                logger.error(vedtaksperiodefinnerSmåbarnstilleggFeil.message)

                return kanIkkeBehandleAutomatisk(
                    behandling = behandlingEtterBehandlingsresultat,
                    metric = antallVedtakOmOvergangsstønadTilManuellBehandling[TilManuellBehandlingÅrsak.KLARER_IKKE_BEGRUNNE]!!,
                    meldingIOppgave = "" // TODO
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

            "påvirker fagsak, autovedtak kjørt vellykket"
        } else {
            antallVedtakOmOvergangsstønadPåvirkerIkkeFagsak.increment()

            "påvirker ikke fagsak"
        }
    }

    private fun begrunnAutovedtakForSmåbarnstillegg(
        behandlingEtterBehandlingsresultat: Behandling
    ) {
        val sistIverksatteBehandling =
            behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = behandlingEtterBehandlingsresultat.fagsak.id)
        val forrigeSmåbarnstilleggAndeler =
            if (sistIverksatteBehandling == null) emptyList()
            else beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(
                behandlingId = sistIverksatteBehandling.id
            ).filter { it.erSmåbarnstillegg() }

        val nyeSmåbarnstilleggAndeler =
            if (sistIverksatteBehandling == null) emptyList()
            else beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(
                behandlingId = behandlingEtterBehandlingsresultat.id
            ).filter { it.erSmåbarnstillegg() }

        val (innvilgedeMånedPerioder, reduserteMånedPerioder) = hentInnvilgedeOgReduserteAndelerSmåbarnstillegg(
            forrigeSmåbarnstilleggAndeler = forrigeSmåbarnstilleggAndeler,
            nyeSmåbarnstilleggAndeler = nyeSmåbarnstilleggAndeler,
        )

        vedtaksperiodeService.lagre(
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
        behandlingService.omgjørTilManuellBehandling(behandling)
        return autovedtakService.opprettOppgaveForManuellBehandling(
            behandling = behandling,
            begrunnelse = meldingIOppgave, // TODO
            oppgavetype = Oppgavetype.BehandleSak,
            opprettLogginnslag = true
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(VedtakOmOvergangsstønadService::class.java)
    }
}
