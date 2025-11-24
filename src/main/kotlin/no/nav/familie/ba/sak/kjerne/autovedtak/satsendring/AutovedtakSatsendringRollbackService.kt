package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.SatsendringFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.SettPåMaskinellVentÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class AutovedtakSatsendringRollbackService(
    private val taskRepository: TaskRepositoryWrapper,
    private val behandlingRepository: BehandlingRepository,
    private val autovedtakService: AutovedtakService,
    private val behandlingService: BehandlingService,
    private val snikeIKøenService: SnikeIKøenService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) {
    val logger = LoggerFactory.getLogger(this::class.java)
    private val satsendringIgnorertÅpenBehandling = Metrics.counter("satsendring.ignorert.aapenbehandling")

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun kjørSatsendring(fagsakId: Long) {
        val sisteVedtatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId)
                ?: throw Feil("Fant ikke siste vedtatte behandling for $fagsakId")

        val aktivOgÅpenBehandling =
            behandlingRepository.findByFagsakAndAktivAndOpen(fagsakId = sisteVedtatteBehandling.fagsak.id)
        val søkerAktør = sisteVedtatteBehandling.fagsak.aktør

        logger.info("Kjører satsendring på $sisteVedtatteBehandling")
        secureLogger.info("Kjører satsendring på $sisteVedtatteBehandling for ${søkerAktør.aktivFødselsnummer()}")

        if (aktivOgÅpenBehandling != null) {
            val brukerHarÅpenBehandlingSvar = hentBrukerHarÅpenBehandlingSvar(aktivOgÅpenBehandling)
            if (brukerHarÅpenBehandlingSvar == SatsendringSvar.BEHANDLING_KAN_SNIKES_FORBI) {
                snikeIKøenService.settAktivBehandlingPåMaskinellVent(
                    aktivOgÅpenBehandling.id,
                    SettPåMaskinellVentÅrsak.SATSENDRING,
                )
            } else {
                satsendringIgnorertÅpenBehandling.increment()
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

                else -> {
                    throw Feil("Ugyldig neste steg ${behandlingEtterBehandlingsresultat.steg} ved satsendring for fagsak=$fagsakId")
                }
            }

        taskRepository.save(task)
    }

    private fun hentBrukerHarÅpenBehandlingSvar(
        aktivOgÅpenBehandling: Behandling,
    ): SatsendringSvar {
        val status = aktivOgÅpenBehandling.status
        return when {
            status != BehandlingStatus.UTREDES && status != BehandlingStatus.SATT_PÅ_VENT -> {
                SatsendringSvar.BEHANDLING_ER_LÅST_SATSENDRING_TRIGGES_NESTE_VIRKEDAG
            }

            snikeIKøenService.kanSnikeForbi(aktivOgÅpenBehandling) -> {
                SatsendringSvar.BEHANDLING_KAN_SNIKES_FORBI
            }

            else -> {
                SatsendringSvar.BEHANDLING_KAN_IKKE_SETTES_PÅ_VENT
            }
        }
    }
}
