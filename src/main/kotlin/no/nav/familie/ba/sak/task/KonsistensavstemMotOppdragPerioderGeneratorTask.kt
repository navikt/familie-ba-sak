package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.integrasjoner.økonomi.AvstemmingService
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingDataTaskDTO
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingPerioderGeneratorTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = KonsistensavstemMotOppdragPerioderGeneratorTask.TASK_STEP_TYPE,
    beskrivelse = "Genererer perioder til Konsistensavstemming",
    maxAntallFeil = 3
)
class KonsistensavstemMotOppdragPerioderGeneratorTask(
    val avstemmingService: AvstemmingService,
    val taskRepository: TaskRepository
) :
    AsyncTaskStep {

    override fun doTask(task: Task) {
        val taskDto =
            objectMapper.readValue(task.payload, KonsistensavstemmingPerioderGeneratorTaskDTO::class.java)

        if (avstemmingService.erKonsistensavstemmingKjørtForTransaksjonsidOgChunk(
                taskDto.transaksjonsId,
                taskDto.chunkNr
            )
        ) return

        val perioderTilAvstemming =
            avstemmingService.hentDataForKonsistensavstemming(
                taskDto.avstemmingsdato,
                taskDto.relevanteBehandlinger
            )

        logger.info("Oppretter konsisensavstemmingstasker for transaksjonsId ${taskDto.transaksjonsId} og chunk ${taskDto.chunkNr} med ${perioderTilAvstemming.size} løpende saker")
        val konsistensavstemmingDataTask = Task(
            type = KonsistensavstemMotOppdragDataTask.TASK_STEP_TYPE,
            payload = objectMapper.writeValueAsString(
                KonsistensavstemmingDataTaskDTO(
                    transaksjonsId = taskDto.transaksjonsId,
                    chunkNr = taskDto.chunkNr,
                    avstemmingdato = taskDto.avstemmingsdato,
                    perioderForBehandling = perioderTilAvstemming
                )
            )
        )
        taskRepository.save(konsistensavstemmingDataTask)
    }

    companion object {
        private val logger: Logger =
            LoggerFactory.getLogger(KonsistensavstemMotOppdragPerioderGeneratorTask::class.java)

        const val TASK_STEP_TYPE = "konsistensavstemMotOppdragPerioderGeneratorTask"
    }
}
