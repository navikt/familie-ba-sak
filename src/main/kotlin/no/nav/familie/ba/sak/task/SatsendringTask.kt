package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.AutovedtakSatsendringService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = SatsendringTask.TASK_STEP_TYPE,
    beskrivelse = "Utfør satsendring",
    maxAntallFeil = 1
)
class SatsendringTask(
    val autovedtakSatsendringService: AutovedtakSatsendringService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val fagsakId = task.payload.toLong()

        autovedtakSatsendringService.kjørBehandling(fagsakId = fagsakId)
    }

    companion object {

        const val TASK_STEP_TYPE = "satsendring"
    }
}
