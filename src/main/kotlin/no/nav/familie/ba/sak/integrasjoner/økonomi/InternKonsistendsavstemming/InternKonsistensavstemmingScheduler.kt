package no.nav.familie.ba.sak.integrasjoner.økonomi.InternKonsistendsavstemming

import no.nav.familie.ba.sak.task.InternKonsistensavstemming.OpprettInternKonsistensavstemmingTaskerTask
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class InternKonsistensavstemmingScheduler(
    val taskService: TaskService
) {

    @Scheduled(cron = "0 0 0 16 * *")
    fun startInternKonsistensavstemming() {
        if (LeaderClient.isLeader() == true) {
            taskService.save(OpprettInternKonsistensavstemmingTaskerTask.opprettTask())
        }
    }
}
