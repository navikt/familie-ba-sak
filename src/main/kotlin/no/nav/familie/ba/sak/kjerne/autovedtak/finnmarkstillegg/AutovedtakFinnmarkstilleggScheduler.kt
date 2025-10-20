package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import no.nav.familie.ba.sak.config.LeaderClientService
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle.AUTOMATISK_KJØRING_AV_AUTOVEDTAK_FINNMARKSTILLEGG
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class AutovedtakFinnmarkstilleggScheduler(
    private val leaderClientService: LeaderClientService,
    private val featureToggleService: FeatureToggleService,
    private val autovedtakFinnmarkstilleggTaskOppretter: AutovedtakFinnmarkstilleggTaskOppretter,
) : ApplicationListener<ContextClosedEvent> {
    @Volatile
    private var isShuttingDown = false

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = CRON_HVERT_5_MIN_UKEDAG)
    fun kjørAutovedtakFinnmarkstillegg() {
        if (isShuttingDown || !leaderClientService.isLeader()) {
            return
        }
        if (featureToggleService.isEnabled(AUTOMATISK_KJØRING_AV_AUTOVEDTAK_FINNMARKSTILLEGG)) {
            logger.info("Starter kjøring av autovedtak finnmarkstillegg")
            autovedtakFinnmarkstilleggTaskOppretter.opprettTasker(50)
        }
    }

    override fun onApplicationEvent(event: ContextClosedEvent) {
        isShuttingDown = true
    }

    companion object {
        const val CRON_HVERT_5_MIN_UKEDAG = "0 */5 7-20 * * MON-FRI"
    }
}
