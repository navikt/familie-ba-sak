package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.integrasjoner.økonomi.AvstemmingService
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingDataTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = KonsistensavstemMotOppdrag.TASK_STEP_TYPE,
    beskrivelse = "Konsistensavstemming mot oppdrag",
    maxAntallFeil = 3
)
class KonsistensavstemMotOppdragData(
    val avstemmingService: AvstemmingService,
    val repository: TaskKonsistensavstemmingRepository
) :
    AsyncTaskStep {

    override fun doTask(task: Task) {
        val konsistensavstemmingDataTask =
            objectMapper.readValue(task.payload, KonsistensavstemmingDataTaskDTO::class.java)

        avstemmingService.konsistensavstemOppdragData(
            avstemmingsdato = konsistensavstemmingDataTask.avstemmingdato,
            perioderTilAvstemming = konsistensavstemmingDataTask.perioderForBehandling,
            transaksjonsId = konsistensavstemmingDataTask.transaksjonsId
        )
    }

    companion object {
        const val TASK_STEP_TYPE = "konsistensavstemMotOppdragData"
    }
}
