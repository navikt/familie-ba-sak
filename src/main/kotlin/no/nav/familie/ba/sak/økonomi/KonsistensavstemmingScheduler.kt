package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdrag
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class KonsistensavstemmingScheduler(val batchService: BatchService,
                                    val behandlingService: BehandlingService,
                                    val fagsakService: FagsakService,
                                    val taskRepository: TaskRepository) {

    @Scheduled(cron = "0 0 17 * * *")
    fun utførKonsistensavstemming() {
        val dagensDato = LocalDate.now()
        val plukketBatch = batchService.plukkLedigeBatchKjøringerFor(dagensDato) ?: return

        fagsakService.hentLøpendeFagsaker().forEach {
            val gjeldendeBehandling = behandlingService.oppdaterGjeldendeBehandlingForFremtidigUtbetaling(it.id, dagensDato)
            if (gjeldendeBehandling.isEmpty()) fagsakService.oppdaterStatus(it, FagsakStatus.STANSET)
        }

        LOG.info("Kjører konsistensavstemming for $dagensDato")

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