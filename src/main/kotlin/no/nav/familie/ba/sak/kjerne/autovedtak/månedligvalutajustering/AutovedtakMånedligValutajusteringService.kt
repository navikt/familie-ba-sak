package no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.StartSatsendring
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.SettPåMaskinellVentÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.måValutakurserOppdateresForMåned
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.prosessering.error.RekjørSenereException
import no.nav.familie.util.VirkedagerProvider.nesteVirkedag
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class AutovedtakMånedligValutajusteringService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val autovedtakService: AutovedtakService,
    private val snikeIKøenService: SnikeIKøenService,
    private val taskRepository: TaskRepositoryWrapper,
    private val behandlingService: BehandlingService,
    private val clockProvider: ClockProvider,
    private val valutakursService: ValutakursService,
    private val simuleringService: SimuleringService,
    private val startSatsendring: StartSatsendring,
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun utførMånedligValutajustering(
        fagsakId: Long,
        måned: YearMonth,
    ) {
        logger.info("Utfører månedlig valutajustering for fagsak=$fagsakId og måned=$måned")

        val sisteVedtatteBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = fagsakId) ?: throw Feil("Fant ikke siste vedtatte behandling for $fagsakId")
        if (sisteVedtatteBehandling.kategori != BehandlingKategori.EØS) {
            logger.warn("Prøver å utføre månedlig valutajustering for nasjonal fagsak $fagsakId. Hopper ut")
            return
        }

        val sisteValutakurser = valutakursService.hentValutakurser(BehandlingId(sisteVedtatteBehandling.id))
        if (!sisteValutakurser.måValutakurserOppdateresForMåned(måned)) {
            logger.info("Valutakursene er allerede oppdatert for fagsak $fagsakId. Hopper ut")
            return
        }

        if (måned != YearMonth.now(clockProvider.get())) {
            throw Feil("Prøver å utføre månedlig valutajustering for en annen måned enn nåværende måned.")
        }

        if (sisteVedtatteBehandling.fagsak.status != FagsakStatus.LØPENDE) {
            throw Feil("Forsøker å utføre månedlig valutajustering på ikke løpende fagsak $fagsakId")
        }

        val harOpprettetSatsendring =
            startSatsendring.sjekkOgOpprettSatsendringVedGammelSats(fagsakId = fagsakId)
        if (harOpprettetSatsendring) {
            throw RekjørSenereException(
                "Satsendring skal kjøre ferdig før man behandler månedlig valutajustering for fagsakId=$fagsakId",
                LocalDateTime.now().plusMinutes(60),
            )
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

                else -> {
                    throw Feil("Ugyldig neste steg ${behandlingEtterBehandlingsresultat.steg} ved månedlig valutajustering for fagsak=$fagsakId")
                }
            }
        taskRepository.save(task)
    }

    private fun validerOgSettÅpenBehandlingPåMaskinellVent(åpenBehandling: Behandling) {
        when (åpenBehandling.status) {
            BehandlingStatus.UTREDES,
            BehandlingStatus.SATT_PÅ_VENT,
            -> {
                if (snikeIKøenService.kanSnikeForbi(åpenBehandling)) {
                    snikeIKøenService.settAktivBehandlingPåMaskinellVent(
                        åpenBehandling.id,
                        SettPåMaskinellVentÅrsak.MÅNEDLIG_VALUTAJUSTERING,
                    )
                } else {
                    throw RekjørSenereException(
                        årsak = "Åpen behandling med status ${åpenBehandling.status} ble endret for under fire timer siden. Prøver igjen klokken 06.00 neste virkedag",
                        triggerTid = nesteVirkedag(LocalDate.now()).atTime(6, 0),
                    )
                }
            }

            BehandlingStatus.IVERKSETTER_VEDTAK,
            BehandlingStatus.SATT_PÅ_MASKINELL_VENT,
            -> {
                throw RekjørSenereException(
                    årsak = "Åpen behandling har status ${åpenBehandling.status}. Prøver igjen om én time",
                    triggerTid = LocalDateTime.now().plusHours(1),
                )
            }

            BehandlingStatus.FATTER_VEDTAK,
            -> {
                val klokken6OmTreVirkedager = (1..3).fold(LocalDate.now()) { acc, _ -> nesteVirkedag(acc) }.atTime(6, 0)
                throw RekjørSenereException(
                    årsak = "Åpen behandling har status ${åpenBehandling.status}. Prøver igjen klokken 06.00 om tre virkedager",
                    triggerTid = klokken6OmTreVirkedager,
                )
            }

            else -> {
                throw Feil("Ikke håndtert feilsituasjon på $åpenBehandling")
            }
        }
    }
}
