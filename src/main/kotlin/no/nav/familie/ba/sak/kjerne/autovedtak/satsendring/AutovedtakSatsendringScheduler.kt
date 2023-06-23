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
        startSatsendring(1200)
    }

    @Scheduled(cron = CRON_HVERT_5_MIN_UKEDAG_UTENFOR_ARBEIDSTID)
    fun triggSatsendringJuli2023UtenforArbeidstid() {
        startSatsendring(1000)
    }

    @Scheduled(cron = CRON_HVERT_5_MIN_LØRDAG)
    fun triggSatsendringJuli2023Lørdag() {
        startSatsendring(1000)
    }

    private fun startSatsendring(antallFagsaker: Int) {
        if (LeaderClient.isLeader() == true) {
            logger.info("Starter schedulert jobb for satsendring juli 2023")
            startSatsendring.startSatsendring(
                antallFagsaker = antallFagsaker,
            )
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakSatsendringScheduler::class.java)
        const val CRON_HVERT_10_MIN_UKEDAG = "0 */10 6-15 * * MON-FRI"
        const val CRON_HVERT_5_MIN_UKEDAG_UTENFOR_ARBEIDSTID = "0 */5 16-20 * * MON-FRI"
        const val CRON_HVERT_5_MIN_LØRDAG = "0 */5 7-17 * * SAT"
    }
}
