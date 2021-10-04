package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.integrasjoner.økonomi.AvstemmingService
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingTaskDTO
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
class KonsistensavstemMotOppdrag(val avstemmingService: AvstemmingService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val konsistensavstemmingTask = objectMapper.readValue(task.payload, KonsistensavstemmingTaskDTO::class.java)

        avstemmingService.konsistensavstemOppdrag(konsistensavstemmingTask.avstemmingdato)
    }

    companion object {
        const val TASK_STEP_TYPE = "konsistensavstemMotOppdrag"
    }
}
