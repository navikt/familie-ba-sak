package no.nav.familie.ba.sak.kjerne.autorevurdering

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SatsendringScheduler(
        private val satsendringService: SatsendringService,
        private val behandlingRepository: BehandlingRepository) {

    /**
     * Jobb som skal kjøres i forbindelse med satsendring som trer i kraft september 2021.
     * For hver behandling det skal utføres satsendring på kjøres en transaksjon som oppretter satsendringbehandling og iverksettingstask.
     */
    @Scheduled(initialDelay = 120000, fixedDelay = Long.MAX_VALUE) // TODO: Sette opp tidspunkt for prod.
    fun gjennomførSatsendring() {
        when (LeaderClient.isLeader()) {
            true -> {
                logger.info("Poden er leader")
                val behandlinger = behandlingRepository.finnBehandlingerSomSkalSatsendresSeptember21()
                logger.info("Starter automatisk revurdering av ${behandlinger.size} behandlinger")

                var vellykkedeRevurderinger = 0
                var mislykkedeRevurderinger = 0

                behandlinger.forEach { behandlingId ->
                    Result.runCatching {
                        satsendringService.utførSatsendring(behandlingId)
                    }.onSuccess {
                        logger.info("Vellykket revurdering for behandling ${behandlingId}")
                        vellykkedeRevurderinger++
                    }.onFailure {
                        logger.info("Revurdering feilet for behandling ${behandlingId}")
                        secureLogger.info("Revurdering feilet for behandling ${behandlingId}", it)
                        mislykkedeRevurderinger++
                    }
                }

                logger.info("Automatiske revurderinger ferdig.\n" +
                            "Antall behandlinger=${behandlinger.size}\n" +
                            "Vellykede=$vellykkedeRevurderinger\n" +
                            "Mislykkede=$mislykkedeRevurderinger\n")
            }
            false -> logger.info("Poden er ikke satt opp som leader")
            null -> logger.info("Poden svarer ikke om den er leader eller ikke")
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(SatsendringScheduler::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}