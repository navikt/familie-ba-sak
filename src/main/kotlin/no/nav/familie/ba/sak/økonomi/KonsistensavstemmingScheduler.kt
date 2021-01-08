package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdrag
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate.now
import java.time.LocalDateTime
import java.time.YearMonth

@Component
class KonsistensavstemmingScheduler(val batchService: BatchService,
                                    val behandlingService: BehandlingService,
                                    val fagsakService: FagsakService,
                                    val taskRepository: TaskRepository) {

    @Scheduled(cron = "0 0 17 * * *")
    fun utførKonsistensavstemming() {
        val inneværendeMåned = YearMonth.from(now()) // TODO: Manuelt sette inn ny kjøredato?
        val plukketBatch = batchService.plukkLedigeBatchKjøringerFor(dato = now()) ?: return

        LOG.info("Kjører konsistensavstemming for $inneværendeMåned")

        val konsistensavstemmingTask = Task.nyTask(
                KonsistensavstemMotOppdrag.TASK_STEP_TYPE,
                objectMapper.writeValueAsString(KonsistensavstemmingTaskDTO(
                        LocalDateTime.now()))
        )
        taskRepository.save(konsistensavstemmingTask)

        batchService.lagreNyStatus(plukketBatch, KjøreStatus.FERDIG)
    }

    companion object {

        val LOG = LoggerFactory.getLogger(KonsistensavstemmingScheduler::class.java)
    }
}

enum class KjøreStatus {
    FERDIG,
    TATT,
    LEDIG
}