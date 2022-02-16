package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.integrasjoner.økonomi.AvstemmingService
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingStartTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = KonsistensavstemMotOppdrag.TASK_STEP_TYPE,
    beskrivelse = "Konsistensavstemming mot oppdrag",
    maxAntallFeil = 3
)
class KonsistensavstemMotOppdragStart(val avstemmingService: AvstemmingService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val konsistensavstemmingTask =
            objectMapper.readValue(task.payload, KonsistensavstemmingStartTaskDTO::class.java)
        val transaksjonsId = UUID.randomUUID()

        avstemmingService.konsistensavstemOppdragStart(
            batchId = konsistensavstemmingTask.batchId,
            avstemmingsdato = konsistensavstemmingTask.avstemmingdato,
            transaksjonsId = transaksjonsId
        )
    }

    companion object {
        const val TASK_STEP_TYPE = "konsistensavstemMotOppdragStart"
    }
}
