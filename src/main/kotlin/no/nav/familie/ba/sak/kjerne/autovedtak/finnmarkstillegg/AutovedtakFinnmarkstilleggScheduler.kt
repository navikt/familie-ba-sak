package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import no.nav.familie.ba.sak.config.LeaderClientService
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle.AUTOMATISK_KJØRING_AV_AUTOVEDTAK_FINNMARKSTILLEGG
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.AutovedtakSatsendringScheduler.Companion.CRON_HVERT_10_MIN_UKEDAG
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

    @Scheduled(cron = CRON_HVERT_10_MIN_UKEDAG)
    fun kjørAutovedtakFinnmarkstillegg() {
        if (isShuttingDown || !leaderClientService.isLeader()) {
            return
        }
        if (featureToggleService.isEnabled(AUTOMATISK_KJØRING_AV_AUTOVEDTAK_FINNMARKSTILLEGG)) {
            autovedtakFinnmarkstilleggTaskOppretter.opprettTasker(1000)
        }
    }

    override fun onApplicationEvent(event: ContextClosedEvent) {
        isShuttingDown = true
    }
}
