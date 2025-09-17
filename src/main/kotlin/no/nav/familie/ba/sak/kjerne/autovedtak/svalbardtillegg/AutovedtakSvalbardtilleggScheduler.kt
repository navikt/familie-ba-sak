package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg

import no.nav.familie.ba.sak.config.LeaderClientService
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.AutovedtakSatsendringScheduler.Companion.CRON_HVERT_10_MIN_UKEDAG
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class AutovedtakSvalbardtilleggScheduler(
    private val leaderClientService: LeaderClientService,
    private val featureToggleService: FeatureToggleService,
    private val autovedtakSvalbardtilleggTaskOppretter: AutovedtakSvalbardtilleggTaskOppretter,
) : ApplicationListener<ContextClosedEvent> {
    @Volatile
    private var isShuttingDown = false

    @Scheduled(cron = CRON_HVERT_10_MIN_UKEDAG)
    fun kjørAutovedtakSvalbardtillegg() {
        if (isShuttingDown || !leaderClientService.isLeader()) {
            return
        }

        if (featureToggleService.isEnabled(FeatureToggle.AUTOMATISK_KJØRING_AV_AUTOVEDTAK_SVALBARDSTILLEGG)) {
            autovedtakSvalbardtilleggTaskOppretter.opprettTasker(1000)
        }
    }

    override fun onApplicationEvent(event: ContextClosedEvent) {
        isShuttingDown = true
    }
}
