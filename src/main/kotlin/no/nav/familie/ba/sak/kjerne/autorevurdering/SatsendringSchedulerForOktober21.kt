package no.nav.familie.ba.sak.kjerne.autorevurdering

import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.net.InetAddress

@Service
class SatsendringSchedulerForOktober21(private val satsendringService: SatsendringService) {

    @Scheduled(cron = "0 0 10 11 10 *")
    fun gjennomførSatsendring() {
        when (LeaderClient.isLeader()) {
            true -> {
                val hostname: String = InetAddress.getLocalHost().hostName
                logger.info("Leaderpod $hostname Starter automatisk revurdering av $behandlingId")

                Result.runCatching {
                    satsendringService.utførSatsendring(behandlingId)
                }.onSuccess {
                    logger.info("Vellykket revurdering med satsendring for behandling")
                }.onFailure {
                    logger.info("Revurdering med satsendring feilet")
                    secureLogger.info("Revurdering feilet for behandling $behandlingId", it)
                }
            }
            false -> logger.info("Poden er ikke satt opp som leader")
            null -> logger.info("Poden svarer ikke om den er leader eller ikke")
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(SatsendringSchedulerForOktober21::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
        private val behandlingId = 1076708L
    }
}