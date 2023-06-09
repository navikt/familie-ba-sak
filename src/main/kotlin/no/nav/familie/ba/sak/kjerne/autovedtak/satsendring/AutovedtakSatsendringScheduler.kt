package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.StartSatsendring.Companion.SATSENDRINGMÅNED_JULI_2023
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.StartSatsendring.Companion.SATSENDRINGMÅNED_MARS_2023
import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class AutovedtakSatsendringScheduler(
    private val startSatsendring: StartSatsendring,
) {

    @Scheduled(cron = CRON_KL_7_OG_14_UKEDAGER)
    @Deprecated("Kan slettes når satsendring for juli 2023 er skrudd på")
    fun triggSatsendringMars2023() {
        if (LeaderClient.isLeader() == true && startSatsendring.hentAktivSatsendringstidspunkt() == SATSENDRINGMÅNED_MARS_2023) {
            logger.info("Starter schedulert jobb for satsendring mars 2023")
            startSatsendring.startSatsendring(
                antallFagsaker = 700,
            )
        }
    }

    @Scheduled(cron = CRON_HVERT_15_MIN_UKEDAG)
    fun triggSatsendringJuli2023() {
        if (LeaderClient.isLeader() == true && startSatsendring.hentAktivSatsendringstidspunkt() == SATSENDRINGMÅNED_JULI_2023) {
            logger.info("Starter schedulert jobb for satsendring juli 2023")
            startSatsendring.startSatsendring(
                antallFagsaker = 100, // Starter med lav fart, men skal skrus opp både denne og hyppigere og evt lengre tidspunkt
            )
        }
    }
    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakSatsendringScheduler::class.java)
        const val CRON_HVERT_15_MIN_UKEDAG = "0 */15 6-19 * * MON-FRI"
        const val CRON_KL_7_OG_14_UKEDAGER = "0 0 7,14 * * MON-FRI"
    }
}
