package no.nav.familie.ba.sak.task

import no.nav.familie.leader.LeaderClient
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
@Profile("!prod")
class FjernGamleFeiledeTasksIPreprod(val taskService: TaskService) {
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    fun fjernGamleFeiledeTasksIPreprod() {
        val isLeader = LeaderClient.isLeader()

        if (isLeader != null && isLeader) {
            LOG.info("Fjerner gamle feilede tasks")

            for (task: Task in taskService.finnTasksMedStatus(listOf(Status.FEILET), null, PageRequest.of(0, 200))) {
                if (task.opprettetTid.isBefore(LocalDateTime.now().minusMonths(1))) {
                    try {
                        MDC.put(MDCConstants.MDC_CALL_ID, task.callId)
                        LOG.info("Sletter gammel feilet task i preprod ${task.id} ${task.type}")
                        taskService.delete(task)
                    } finally {
                        MDC.clear()
                    }
                }
            }
        }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(FjernGamleFeiledeTasksIPreprod::class.java)
    }
}
