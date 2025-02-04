package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.LeaderClientService
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class AutovedtakSatsendringScheduler(
    private val startSatsendring: StartSatsendring,
    private val unleashService: UnleashNextMedContextService,
    private val leaderClientService: LeaderClientService,
) {
    @Scheduled(cron = CRON_HVERT_10_MIN_UKEDAG)
    fun triggSatsendring() {
        if (unleashService.isEnabled(FeatureToggle.SATSENDRING_HØYT_VOLUM, false)) {
            startSatsendring(1200)
        } else {
            startSatsendring(100)
        }
    }

    @Scheduled(cron = CRON_HVERT_5_MIN_UKEDAG_UTENFOR_ARBEIDSTID)
    fun triggSatsendringUtenforArbeidstid() {
        if (unleashService.isEnabled(FeatureToggle.SATSENDRING_KVELD, false)) {
            startSatsendring(1000)
        }
    }

    @Scheduled(cron = CRON_HVERT_5_MIN_LØRDAG)
    fun triggSatsendringLørdag() {
        if (unleashService.isEnabled(FeatureToggle.SATSENDRING_LØRDAG, false)) {
            startSatsendring(1000)
        }
    }

    private fun startSatsendring(antallFagsaker: Int) {
        if (leaderClientService.isLeader()) {
            logger.info("Starter schedulert jobb for satsendring 2024-01. antallFagsaker=$antallFagsaker")
            startSatsendring.startSatsendring(
                antallFagsaker = antallFagsaker,
            )
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakSatsendringScheduler::class.java)
        const val CRON_HVERT_10_MIN_UKEDAG = "0 */10 7-18 * * MON-FRI"
        const val CRON_HVERT_5_MIN_UKEDAG_UTENFOR_ARBEIDSTID = "0 */5 16-20 * * MON-FRI"
        const val CRON_HVERT_5_MIN_LØRDAG = "0 */5 7-17 * * SAT"
    }
}
