package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdrag
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate.now
import java.time.LocalDateTime
import java.time.YearMonth

@Component
class KonsistensavstemmingScheduler(
    val batchService: BatchService,
    val behandlingService: BehandlingService,
    val fagsakService: FagsakService,
    val taskRepository: TaskRepositoryWrapper
) {

    @Scheduled(cron = "0 0 17 * * *")
    fun utførKonsistensavstemming() {
        val inneværendeMåned = YearMonth.from(now())
        val plukketBatch = batchService.plukkLedigeBatchKjøringerFor(dato = now()) ?: return

        logger.info("Kjører konsistensavstemming for $inneværendeMåned")

        val konsistensavstemmingTask = Task(
            type = KonsistensavstemMotOppdrag.TASK_STEP_TYPE,
            payload = objectMapper.writeValueAsString(
                KonsistensavstemmingTaskDTO(
                    LocalDateTime.now()
                )
            )
        )
        taskRepository.save(konsistensavstemmingTask)

        batchService.lagreNyStatus(plukketBatch, KjøreStatus.FERDIG)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(KonsistensavstemmingScheduler::class.java)
    }
}

enum class KjøreStatus {
    FERDIG,
    TATT,
    LEDIG
}
