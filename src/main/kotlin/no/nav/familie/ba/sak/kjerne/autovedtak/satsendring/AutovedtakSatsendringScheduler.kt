package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class AutovedtakSatsendringScheduler(private val startSatsendring: StartSatsendring) {

    @Scheduled(cron = CRON_HVERT_30_MIN_ARBEIDSTID_UKEDAG)
    fun triggSatsendring() {
        if (LeaderClient.isLeader() == true) {
            logger.info("Satsendring trigges av schedulert jobb")
            startSatsendring.startSatsendring(
                antallFagsaker = 200,
                satsTidspunkt = StartSatsendring.SATSENDRINGMÅNED_2023
            )
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakSatsendringScheduler::class.java)
        const val CRON_HVERT_5_MIN_ARBEIDSTID_UKEDAG = "0 */5 7-17 * * MON-FRI"
        const val CRON_HVERT_30_MIN_ARBEIDSTID_UKEDAG = "0 */30 7-17 * * MON-FRI"
    }
}
