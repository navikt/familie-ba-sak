package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.SatsendringFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.SettPåMaskinellVentÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AutovedtakSatsendringRollbackService(
    private val taskRepository: TaskRepositoryWrapper,
    private val behandlingRepository: BehandlingRepository,
    private val autovedtakService: AutovedtakService,
    private val satskjøringRepository: SatskjøringRepository,
    private val behandlingService: BehandlingService,
    private val satsendringService: SatsendringService,
    private val loggService: LoggService,
    private val snikeIKøenService: SnikeIKøenService,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun kjørSatsendring(fagsakId: Long) {
        val sisteVedtatteBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId) ?: error("Fant ikke siste vedtatte behandling for $fagsakId")

        val aktivOgÅpenBehandling =
            behandlingRepository.findByFagsakAndAktivAndOpen(fagsakId = sisteVedtatteBehandling.fagsak.id)
        val søkerAktør = sisteVedtatteBehandling.fagsak.aktør

        logger.info("Kjører satsendring på $sisteVedtatteBehandling")
        secureLogger.info("Kjører satsendring på $sisteVedtatteBehandling for ${søkerAktør.aktivFødselsnummer()}")

        if (aktivOgÅpenBehandling != null) {
            val brukerHarÅpenBehandlingSvar = hentBrukerHarÅpenBehandlingSvar(aktivOgÅpenBehandling)
            if (brukerHarÅpenBehandlingSvar == SatsendringSvar.BEHANDLING_KAN_SNIKES_FORBI) {
                snikeIKøenService.settAktivBehandlingTilPåMaskinellVent(
                    aktivOgÅpenBehandling.id,
                    SettPåMaskinellVentÅrsak.SATSENDRING,
                )
            } else {
                throw SatsendringFeil(satsendringSvar = brukerHarÅpenBehandlingSvar)
            }
        }

        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                aktør = søkerAktør,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.SATSENDRING,
                fagsakId = sisteVedtatteBehandling.fagsak.id,
            )

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

                else -> throw Feil("Ugyldig neste steg ${behandlingEtterBehandlingsresultat.steg} ved satsendring for fagsak=$fagsakId")
            }

        taskRepository.save(task)
    }

    private fun hentBrukerHarÅpenBehandlingSvar(
        aktivOgÅpenBehandling: Behandling,
    ): SatsendringSvar {
        val status = aktivOgÅpenBehandling.status
        return when {
            status != BehandlingStatus.UTREDES && status != BehandlingStatus.SATT_PÅ_VENT ->
                SatsendringSvar.BEHANDLING_ER_LÅST_SATSENDRING_TRIGGES_NESTE_VIRKEDAG

            kanSnikeIKøen(aktivOgÅpenBehandling) -> SatsendringSvar.BEHANDLING_KAN_SNIKES_FORBI
            else -> SatsendringSvar.BEHANDLING_KAN_IKKE_SETTES_PÅ_VENT
        }
    }

    private fun kanSnikeIKøen(aktivOgÅpenBehandling: Behandling): Boolean {
        val behandlingId = aktivOgÅpenBehandling.id
        val loggSuffix = "endrer status på behandling til på vent"
        if (aktivOgÅpenBehandling.status == BehandlingStatus.SATT_PÅ_VENT) {
            AutovedtakSatsendringService.logger.info("Behandling=$behandlingId er satt på vent av saksbehandler, $loggSuffix")
            return true
        }
        val sisteLogghendelse = loggService.hentLoggForBehandling(behandlingId).maxBy { it.opprettetTidspunkt }
        val tid4TimerSiden = LocalDateTime.now().minusHours(4)
        if (aktivOgÅpenBehandling.endretTidspunkt.isAfter(tid4TimerSiden)) {
            AutovedtakSatsendringService.logger.info(
                "Behandling=$behandlingId har endretTid=${aktivOgÅpenBehandling.endretTidspunkt} " +
                    "kan ikke sette behandlingen på maskinell vent",
            )
            return false
        }
        if (sisteLogghendelse.opprettetTidspunkt.isAfter(tid4TimerSiden)) {
            AutovedtakSatsendringService.logger.info(
                "Behandling=$behandlingId siste logginslag er " +
                    "type=${sisteLogghendelse.type} tid=${sisteLogghendelse.opprettetTidspunkt}, $loggSuffix",
            )
            return false
        }
        return true
    }
}
