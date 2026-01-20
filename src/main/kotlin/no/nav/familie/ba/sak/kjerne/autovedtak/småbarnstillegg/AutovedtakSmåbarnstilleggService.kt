package no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakBehandlingService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.SmåbarnstilleggData
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.HenleggBehandlingInfoDto
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.overgangsstønad.OvergangsstønadService
import no.nav.familie.ba.sak.kjerne.grunnlag.overgangsstønad.erEndringIOvergangsstønadFramITid
import no.nav.familie.ba.sak.kjerne.småbarnstillegg.SmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class AutovedtakSmåbarnstilleggService(
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val vedtakService: VedtakService,
    private val behandlingService: BehandlingService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val overgangsstønadService: OvergangsstønadService,
    private val taskService: TaskService,
    private val autovedtakService: AutovedtakService,
    private val oppgaveService: OppgaveService,
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    private val clockProvider: ClockProvider,
    private val påVentService: SettPåVentService,
    private val stegService: StegService,
    private val småbarnstilleggService: SmåbarnstilleggService,
) : AutovedtakBehandlingService<SmåbarnstilleggData> {
    private val antallVedtakOmOvergangsstønad: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "antall", "aarsak", "ikke_relevant", "beskrivelse", "ikke_relevant")
    private val antallVedtakOmOvergangsstønadPåvirkerFagsak: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "paavirker_fagsak", "aarsak", "ikke_relevant", "beskrivelse", "ikke_relevant")
    private val antallVedtakOmOvergangsstønadPåvirkerIkkeFagsak: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "paavirker_ikke_fagsak", "aarsak", "ikke_relevant", "beskrivelse", "ikke_relevant")

    enum class TilManuellBehandlingÅrsak(
        val beskrivelse: String,
    ) {
        NYE_UTBETALINGSPERIODER_FØRER_TIL_MANUELL_BEHANDLING("Endring i OS gir etterbetaling, feilutbetaling eller endring mer enn 1 måned frem i tid"),
        KLARER_IKKE_BEGRUNNE("Klarer ikke å begrunne"),
    }

    val antallVedtakOmOvergangsstønadTilManuellBehandling: Map<TilManuellBehandlingÅrsak, Counter> =
        TilManuellBehandlingÅrsak.entries.associateWith {
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

    override fun skalAutovedtakBehandles(behandlingsdata: SmåbarnstilleggData): Boolean {
        val fagsak = fagsakService.hentNormalFagsak(aktør = behandlingsdata.aktør) ?: return false
        val påvirkerFagsak = overgangsstønadService.vedtakOmOvergangsstønadPåvirkerFagsak(fagsak)
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
    override fun kjørBehandling(behandlingsdata: SmåbarnstilleggData): String {
        antallVedtakOmOvergangsstønad.increment()
        val aktør = behandlingsdata.aktør
        val fagsak =
            fagsakService.hentNormalFagsak(aktør)
                ?: throw Feil(message = "Fant ikke fagsak av typen NORMAL for aktør ${aktør.aktørId}")
        val forrigeBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id)
        val perioderMedFullOvergangsstønadForrigeBehandling = forrigeBehandling?.let { overgangsstønadService.hentPerioderMedFullOvergangsstønad(forrigeBehandling) } ?: emptyList()
        val perioderMedFullOvergangsstønad = overgangsstønadService.hentPerioderMedFullOvergangsstønad(aktør)

        val erEndringIOvergangsstønadFramITid =
            erEndringIOvergangsstønadFramITid(
                perioderMedFullOvergangsstønadForrigeBehandling = perioderMedFullOvergangsstønadForrigeBehandling,
                perioderMedFullOvergangsstønad = perioderMedFullOvergangsstønad,
                dagensDato = LocalDate.now(clockProvider.get()),
            )

        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                aktør = aktør,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = if (erEndringIOvergangsstønadFramITid) BehandlingÅrsak.SMÅBARNSTILLEGG_ENDRING_FRAM_I_TID else BehandlingÅrsak.SMÅBARNSTILLEGG,
                fagsakId = fagsak.id,
            )

        if (behandlingEtterBehandlingsresultat.status != BehandlingStatus.IVERKSETTER_VEDTAK) {
            return kanIkkeBehandleAutomatisk(
                automatiskBehandling = behandlingEtterBehandlingsresultat,
                metric = antallVedtakOmOvergangsstønadTilManuellBehandling[TilManuellBehandlingÅrsak.NYE_UTBETALINGSPERIODER_FØRER_TIL_MANUELL_BEHANDLING]!!,
                meldingIOppgave = "Småbarnstillegg: endring i overgangsstønad må behandles manuelt",
            )
        }

        try {
            if (behandlingEtterBehandlingsresultat.erBehandlingMedVedtaksbrevutsending()) {
                begrunnAutovedtakForSmåbarnstillegg(behandlingEtterBehandlingsresultat)
            }
        } catch (e: VedtaksperiodefinnerSmåbarnstilleggFeil) {
            logger.warn(e.message, e)

            val behandlingSomSkalManueltBehandles =
                behandlingService.oppdaterStatusPåBehandling(
                    behandlingEtterBehandlingsresultat.id,
                    BehandlingStatus.UTREDES,
                )
            return kanIkkeBehandleAutomatisk(
                automatiskBehandling = behandlingSomSkalManueltBehandles,
                metric = antallVedtakOmOvergangsstønadTilManuellBehandling[TilManuellBehandlingÅrsak.KLARER_IKKE_BEGRUNNE]!!,
                meldingIOppgave = "Småbarnstillegg: klarer ikke bestemme vedtaksperiode som skal begrunnes, må behandles manuelt",
            )
        }

        val vedtakEtterTotrinn =
            autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterBehandlingsresultat,
            )

        val task =
            IverksettMotOppdragTask.opprettTask(
                behandlingEtterBehandlingsresultat,
                vedtakEtterTotrinn,
                SikkerhetContext.hentSaksbehandler(),
            )
        taskService.save(task)

        return AutovedtakStegService.BEHANDLING_FERDIG
    }

    private fun begrunnAutovedtakForSmåbarnstillegg(
        behandlingEtterBehandlingsresultat: Behandling,
    ) {
        val sistIverksatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = behandlingEtterBehandlingsresultat.fagsak.id)

        val (innvilgedeMånedPerioder, reduserteMånedPerioder) =
            småbarnstilleggService.finnInnvilgedeOgReduserteAndelerSmåbarnstillegg(
                behandling = behandlingEtterBehandlingsresultat,
                sistIverksatteBehandling = sistIverksatteBehandling,
            )

        vedtaksperiodeHentOgPersisterService.lagre(
            finnAktuellVedtaksperiodeOgLeggTilSmåbarnstilleggbegrunnelse(
                innvilgetMånedPeriode = innvilgedeMånedPerioder.singleOrNull(),
                redusertMånedPeriode = reduserteMånedPerioder.singleOrNull(),
                vedtaksperioderMedBegrunnelser =
                    vedtaksperiodeService.hentPersisterteVedtaksperioder(
                        vedtak =
                            vedtakService.hentAktivForBehandlingThrows(
                                behandlingId = behandlingEtterBehandlingsresultat.id,
                            ),
                    ),
            ),
        )
    }

    @Transactional
    fun kanIkkeBehandleAutomatisk(
        automatiskBehandling: Behandling,
        metric: Counter,
        meldingIOppgave: String,
    ): String {
        metric.increment()

        val behandlingPåMaskinellVent =
            behandlingHentOgPersisterService
                .hentBehandlinger(automatiskBehandling.fagsak.id, BehandlingStatus.SATT_PÅ_MASKINELL_VENT)
                .singleOrNull()

        val manuellBehandlingId =
            if (behandlingPåMaskinellVent != null) {
                stegService.håndterHenleggBehandling(
                    behandling = automatiskBehandling,
                    henleggBehandlingInfo =
                        HenleggBehandlingInfoDto(
                            årsak = HenleggÅrsak.AUTOMATISK_HENLAGT,
                            begrunnelse = "Småbarnstillegg: endring i overgangsstønad må behandles manuelt",
                        ),
                )
                taBehandlingAvMaskinellVent(behandlingPåMaskinellVent.id).id
            } else {
                autovedtakService
                    .omgjørBehandlingTilManuellOgKjørSteg(
                        behandling = automatiskBehandling,
                        steg = StegType.VILKÅRSVURDERING,
                    ).id
            }

        oppgaveService.opprettOppgaveForManuellBehandling(
            behandlingId = manuellBehandlingId,
            begrunnelse = meldingIOppgave,
            opprettLogginnslag = true,
            manuellOppgaveType = ManuellOppgaveType.SMÅBARNSTILLEGG,
        )
        return meldingIOppgave
    }

    private fun taBehandlingAvMaskinellVent(behandlingPåMaskinellVentId: Long): Behandling {
        val erBehandlingTilMaskinellVentOgsåPåVent = påVentService.finnAktivSettPåVentPåBehandling(behandlingPåMaskinellVentId) != null

        val status =
            if (erBehandlingTilMaskinellVentOgsåPåVent) {
                BehandlingStatus.SATT_PÅ_VENT
            } else {
                BehandlingStatus.UTREDES
            }

        return behandlingService.oppdaterStatusPåBehandling(behandlingPåMaskinellVentId, status)
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakSmåbarnstilleggService::class.java)
    }
}
