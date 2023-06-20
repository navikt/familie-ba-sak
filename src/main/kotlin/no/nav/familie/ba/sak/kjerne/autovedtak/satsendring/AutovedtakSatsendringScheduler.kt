package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class AutovedtakSatsendringScheduler(
    private val startSatsendring: StartSatsendring,
) {
    @Scheduled(cron = CRON_HVERT_10_MIN_UKEDAG)
    fun triggSatsendringJuli2023() {
        if (LeaderClient.isLeader() == true) {
            logger.info("Starter schedulert jobb for satsendring juli 2023")
            startSatsendring.startSatsendring(
                antallFagsaker = 1200,
            )
        }
    }

    @Scheduled(cron = CRON_HVERT_5_MIN_UKEDAG_UTENFOR_ARBEIDSTID)
    fun triggSatsendringJuli2023UtenforArbeidstid() {
        if (LeaderClient.isLeader() == true) {
            logger.info("Starter schedulert jobb for satsendring juli 2023")
            startSatsendring.startSatsendring(
                antallFagsaker = 1500,
            )
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakSatsendringScheduler::class.java)
        const val CRON_HVERT_10_MIN_UKEDAG = "0 */10 7-15 * * MON-FRI"
        const val CRON_HVERT_5_MIN_UKEDAG_UTENFOR_ARBEIDSTID = "0 */5 6,16-20 * * MON-FRI"
    }
}
