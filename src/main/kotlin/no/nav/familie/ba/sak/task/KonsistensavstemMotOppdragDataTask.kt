package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.integrasjoner.økonomi.AvstemmingService
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingDataTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = KonsistensavstemMotOppdragDataTask.TASK_STEP_TYPE,
    beskrivelse = "Send batcher av Konsistensavstemming mot oppdrag",
    maxAntallFeil = 3
)
class KonsistensavstemMotOppdragDataTask(
    val avstemmingService: AvstemmingService
) :
    AsyncTaskStep {

    override fun doTask(task: Task) {
        val konsistensavstemmingDataTask =
            objectMapper.readValue(task.payload, KonsistensavstemmingDataTaskDTO::class.java)

        if (konsistensavstemmingDataTask.sendTilØkonomi) {
            avstemmingService.konsistensavstemOppdragData(
                avstemmingsdato = konsistensavstemmingDataTask.avstemmingdato,
                perioderTilAvstemming = konsistensavstemmingDataTask.perioderForBehandling,
                transaksjonsId = konsistensavstemmingDataTask.transaksjonsId,
                chunkNr = konsistensavstemmingDataTask.chunkNr
            )
        } else {
            logger.info("Send til økonomi skrudd av for ${konsistensavstemmingDataTask.transaksjonsId} for task $TASK_STEP_TYPE")
        }
    }

    companion object {
        const val TASK_STEP_TYPE = "konsistensavstemMotOppdragData"
        private val logger: Logger =
            LoggerFactory.getLogger(KonsistensavstemMotOppdragDataTask::class.java)
    }
}
