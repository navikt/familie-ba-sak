package no.nav.familie.ba.sak.integrasjoner.skyggesak

import no.nav.familie.ba.sak.config.LeaderClientService
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SkyggesakScheduler(
    val leaderClientService: LeaderClientService,
    val skyggesakService: SkyggesakService,
) : ApplicationListener<ContextClosedEvent> {
    @Volatile private var isShuttingDown = false

    @Scheduled(fixedDelay = 60000)
    fun opprettSkyggesaker() {
        if (!isShuttingDown && leaderClientService.isLeader()) {
            skyggesakService.sendSkyggesaker()
        }
    }

    @Scheduled(cron = "0 0 6 * * *")
    fun ryddOppISendteSkyggesaker() {
        if (leaderClientService.isLeader()) {
            skyggesakService.fjernGamleSkyggesakInnslag()
        }
    }

    override fun onApplicationEvent(event: ContextClosedEvent) {
        isShuttingDown = true
    }
}
