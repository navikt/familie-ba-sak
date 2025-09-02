package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.config.LeaderClientService
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class AutovedtakSatsendringScheduler(
    private val startSatsendring: StartSatsendring,
    private val featureToggleService: FeatureToggleService,
    private val leaderClientService: LeaderClientService,
) : ApplicationListener<ContextClosedEvent> {
    @Volatile private var isShuttingDown = false

    @Scheduled(cron = CRON_HVERT_10_MIN_UKEDAG)
    fun triggSatsendring() {
        if (featureToggleService.isEnabled(FeatureToggle.SATSENDRING_HØYT_VOLUM, false)) {
            startSatsendring(1200)
        } else {
            startSatsendring(100)
        }
    }

    @Scheduled(cron = CRON_HVERT_5_MIN_UKEDAG_UTENFOR_ARBEIDSTID)
    fun triggSatsendringUtenforArbeidstid() {
        if (featureToggleService.isEnabled(FeatureToggle.SATSENDRING_KVELD, false)) {
            startSatsendring(1000)
        }
    }

    @Scheduled(cron = CRON_HVERT_5_MIN_LØRDAG)
    fun triggSatsendringLørdag() {
        if (featureToggleService.isEnabled(FeatureToggle.SATSENDRING_LØRDAG, false)) {
            startSatsendring(1000)
        }
    }

    @Scheduled(cron = CRON_HVER_HVERDAG)
    fun slettFeiledeSatsendringHverdager() {
        startSatsendring.slettFeiledeSatsendringer()
    }

    private fun startSatsendring(antallFagsaker: Int) {
        if (!isShuttingDown && leaderClientService.isLeader()) {
            logger.info("Starter schedulert jobb for satsendring ${StartSatsendring.hentAktivSatsendringstidspunkt()}. antallFagsaker=$antallFagsaker")
            startSatsendring.startSatsendring(
                antallFagsaker = antallFagsaker,
            )
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakSatsendringScheduler::class.java)
        const val CRON_HVERT_10_MIN_UKEDAG = "0 */10 7-15 * * MON-FRI"
        const val CRON_HVERT_5_MIN_UKEDAG_UTENFOR_ARBEIDSTID = "0 */5 16-20 * * MON-FRI"
        const val CRON_HVERT_5_MIN_LØRDAG = "0 */5 7-17 * * SAT"
        const val CRON_HVER_HVERDAG = "0 0 7 * * MON-FRI"
    }

    override fun onApplicationEvent(event: ContextClosedEvent) {
        isShuttingDown = true
    }
}
