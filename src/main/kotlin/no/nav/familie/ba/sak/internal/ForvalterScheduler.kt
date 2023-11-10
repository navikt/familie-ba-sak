package no.nav.familie.ba.sak.internal

import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.task.FinnSakerMedFlereMigreringsbehandlingerTask
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.YearMonth

@Component
class ForvalterScheduler(
    private val taskRepository: TaskRepositoryWrapper,
    private val envService: EnvService,
) {
    /*
     * Oppretter task månedlig for å identifisere løpende fagsaker som har flere migreringer sist måned
     */
    @Scheduled(cron = "0 0 7 1 * *")
    fun opprettFinnSakerMedFlereMigreringsbehandlingerTask() {
        when (LeaderClient.isLeader() == true || envService.erDev()) {
            true -> {
                val finnSakerMedFlereMigreringsbehandlingerTask =
                    Task(
                        type = FinnSakerMedFlereMigreringsbehandlingerTask.TASK_STEP_TYPE,
                        payload = "${YearMonth.now().minusMonths(1)}",
                    )
                taskRepository.save(finnSakerMedFlereMigreringsbehandlingerTask)
                logger.info("Opprettet FinnSakerMedFlereMigreringsbehandlingerTask")
            }

            false -> {
                logger.info("Ikke opprettet FinnSakerMedFlereMigreringsbehandlingerTask på denne poden")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ForvalterScheduler::class.java)
    }
}
