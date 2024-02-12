package no.nav.familie.ba.sak.integrasjoner.økonomi.internkonsistensavstemming

import no.nav.familie.ba.sak.config.LeaderClientService
import no.nav.familie.ba.sak.task.internkonsistensavstemming.OpprettInternKonsistensavstemmingTaskerTask
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class InternKonsistensavstemmingScheduler(
    val taskService: TaskService,
    val leaderClientService: LeaderClientService,
) {
    @Scheduled(cron = "0 0 0 29 * *")
    fun startInternKonsistensavstemming() {
        if (leaderClientService.isLeader() == true) {
            taskService.save(OpprettInternKonsistensavstemmingTaskerTask.opprettTask())
        }
    }
}
