﻿package no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.LocalDateProvider
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.SettPåMaskinellVentÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.erAlleValutakurserOppdaterteIMåned
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.prosessering.error.RekjørSenereException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@Service
class AutovedtakMånedligValutajusteringService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val autovedtakService: AutovedtakService,
    private val snikeIKøenService: SnikeIKøenService,
    private val taskRepository: TaskRepositoryWrapper,
    private val behandlingService: BehandlingService,
    private val localDateProvider: LocalDateProvider,
    private val valutakursService: ValutakursService,
    private val simuleringService: SimuleringService,
) {
    private val månedligvalutajusteringIgnorertÅpenBehandling = Metrics.counter("valutajustering.ignorert.aapenbehandling")

    val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun utførMånedligValutajustering(
        fagsakId: Long,
        måned: YearMonth,
    ) {
        logger.info("Utfører månedlig valutajustering for fagsak=$fagsakId og måned=$måned")

        if (måned != localDateProvider.now().toYearMonth()) {
            throw Feil("Prøver å utføre månedlig valutajustering for en annen måned enn nåværende måned.")
        }

        val sisteVedtatteBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = fagsakId) ?: error("Fant ikke siste vedtatte behandling for $fagsakId")
        val sisteValutakurser = valutakursService.hentValutakurser(BehandlingId(sisteVedtatteBehandling.id))
        if (sisteValutakurser.erAlleValutakurserOppdaterteIMåned(måned)) {
            logger.info("Valutakursene er allerede oppdatert for fagsak $fagsakId. Hopper ut")
            return
        }

        if (sisteVedtatteBehandling.fagsak.status != FagsakStatus.LØPENDE) {
            throw Feil("Forsøker å utføre satsendring på ikke løpende fagsak $fagsakId")
        }

        val aktivOgÅpenBehandling = behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsakId = fagsakId)

        if (aktivOgÅpenBehandling != null) {
            validerOgSettÅpenBehandlingPåMaskinellVent(aktivOgÅpenBehandling)
        }

        val søkerAktør = sisteVedtatteBehandling.fagsak.aktør

        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                aktør = søkerAktør,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.MÅNEDLIG_VALUTAJUSTERING,
                fagsakId = fagsakId,
            )

        simuleringService.oppdaterSimuleringPåBehandlingVedBehov(behandlingEtterBehandlingsresultat.id)

        val etterutbetaling = simuleringService.hentEtterbetaling(behandlingEtterBehandlingsresultat.id)
        val feilutbetaling by lazy { simuleringService.hentFeilutbetaling(behandlingEtterBehandlingsresultat.id) }

        if (etterutbetaling > BigDecimal.ZERO || feilutbetaling > BigDecimal.ZERO) {
            throw Feil("Etterbetaling eller feilutbetaling er større enn 0 ved månedlig valutajustering for fagsak=$fagsakId. Må behandles manuelt.")
        }

        val opprettetVedtak =
            autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterBehandlingsresultat,
            )

        val task =
            when (behandlingEtterBehandlingsresultat.steg) {
                StegType.IVERKSETT_MOT_OPPDRAG -> {
                    IverksettMotOppdragTask.opprettTask(
                        behandlingEtterBehandlingsresultat,
                        opprettetVedtak,
                        SikkerhetContext.hentSaksbehandler(),
                    )
                }

                StegType.FERDIGSTILLE_BEHANDLING -> {
                    behandlingService.oppdaterStatusPåBehandling(
                        behandlingEtterBehandlingsresultat.id,
                        BehandlingStatus.IVERKSETTER_VEDTAK,
                    )
                    FerdigstillBehandlingTask.opprettTask(
                        søkerAktør.aktivFødselsnummer(),
                        behandlingEtterBehandlingsresultat.id,
                    )
                }

                else -> throw Feil("Ugyldig neste steg ${behandlingEtterBehandlingsresultat.steg} ved månedlig valutajustering for fagsak=$fagsakId")
            }
        taskRepository.save(task)
    }

    private fun validerOgSettÅpenBehandlingPåMaskinellVent(åpenBehandling: Behandling) {
        when (åpenBehandling.status) {
            BehandlingStatus.UTREDES,
            BehandlingStatus.SATT_PÅ_VENT,
            ->
                if (snikeIKøenService.kanSnikeForbi(åpenBehandling)) {
                    try {
                        snikeIKøenService.settAktivBehandlingPåMaskinellVent(
                            åpenBehandling.id,
                            SettPåMaskinellVentÅrsak.MÅNEDLIG_VALUTAJUSTERING,
                        )
                    } catch (e: Exception) {
                        throw Feil("Behandling ${åpenBehandling.id} er ikke aktiv")
                    }
                } else {
                    throw RekjørSenereException(
                        årsak = "Åpen behandling med status ${åpenBehandling.status} ble endret for under fire timer siden. Prøver igjen klokken 06.00 i morgen",
                        triggerTid = LocalDate.now().plusDays(1).atTime(6, 0),
                    )
                }

            BehandlingStatus.IVERKSETTER_VEDTAK,
            BehandlingStatus.SATT_PÅ_MASKINELL_VENT,
            -> throw RekjørSenereException(
                årsak = "Åpen behandling har status ${åpenBehandling.status}. Prøver igjen om én time",
                triggerTid = LocalDateTime.now().plusMinutes(119).truncatedTo(ChronoUnit.HOURS), // Nærmeste hele time minst en time frem i tid
            )

            BehandlingStatus.FATTER_VEDTAK,
            -> throw RekjørSenereException(
                årsak = "Åpen behandling har status ${åpenBehandling.status}. Prøver igjen klokken 06.00 i morgen",
                triggerTid = LocalDate.now().plusDays(1).atTime(6, 0),
            )

            else -> throw Feil("Ikke håndtert feilsituasjon på $åpenBehandling")
        }
    }
}
