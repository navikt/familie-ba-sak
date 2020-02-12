package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdrag
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class KonsistensavstemmingScheduler(val taskRepository: TaskRepository,
                                    val batchRepository: BatchRepository) {

    @Scheduled(cron = "0 0 8 * * *")
    fun utførKonsistensavstemming() {
        val dagensDato = LocalDate.now()
        val ledigBatch = batchRepository.findByKjøredatoAndLedig(dagensDato) ?: return

        LOG.info("Kjører konsistensavstemming for $dagensDato")
        ledigBatch.status = KjøreStatus.TATT
        batchRepository.save(ledigBatch)

        val konsistensavstemmingTask = Task.nyTask(
                KonsistensavstemMotOppdrag.TASK_STEP_TYPE,
                objectMapper.writeValueAsString(KonsistensavstemmingTaskDTO(LocalDateTime.now()))
        )
        taskRepository.save(konsistensavstemmingTask)
    }

    companion object {
        val LOG = LoggerFactory.getLogger(KonsistensavstemmingScheduler::class.java)
    }
}

enum class KjøreStatus {
    TATT,
    LEDIG
}