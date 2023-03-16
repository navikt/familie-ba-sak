package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class AutovedtakSatsendringScheduler(private val startSatsendring: StartSatsendring) {

    @Scheduled(cron = CRON_KL_7_OG_14_UKEDAGER)
    fun triggSatsendring() {
        if (LeaderClient.isLeader() == true) {
            logger.info("Satsendring trigges av schedulert jobb")
            startSatsendring.startSatsendring(
                antallFagsaker = 700,
                satsTidspunkt = StartSatsendring.SATSENDRINGMÅNED_2023
            )
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakSatsendringScheduler::class.java)
        const val CRON_KL_7_OG_14_UKEDAGER = "0 0 7,14 * * MON-FRI"
    }
}
