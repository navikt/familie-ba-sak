package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.integrasjoner.økonomi.AvstemmingService
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingDataTaskDTO
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = KonsistensavstemMotOppdragDataTask.TASK_STEP_TYPE,
    beskrivelse = "Send batcher av Konsistensavstemming mot oppdrag",
    maxAntallFeil = 3,
)
class KonsistensavstemMotOppdragDataTask(
    val avstemmingService: AvstemmingService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val konsistensavstemmingDataTask =
            jsonMapper.readValue(task.payload, KonsistensavstemmingDataTaskDTO::class.java)

        avstemmingService.konsistensavstemOppdragData(
            avstemmingsdato = konsistensavstemmingDataTask.avstemmingdato,
            perioderTilAvstemming = konsistensavstemmingDataTask.perioderForBehandling,
            transaksjonsId = konsistensavstemmingDataTask.transaksjonsId,
            chunkNr = konsistensavstemmingDataTask.chunkNr,
            sendTilØkonomi = konsistensavstemmingDataTask.sendTilØkonomi,
        )
    }

    companion object {
        const val TASK_STEP_TYPE = "konsistensavstemMotOppdragData"
    }
}
