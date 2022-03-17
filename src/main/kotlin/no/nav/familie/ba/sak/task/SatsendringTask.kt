package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.AutovedtakSatsendringService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = SatsendringTask.TASK_STEP_TYPE,
    beskrivelse = "Utfør satsendring",
    maxAntallFeil = 1
)
class SatsendringTask(
    val autovedtakSatsendringService: AutovedtakSatsendringService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = task.payload.toLong()

        autovedtakSatsendringService.kjørBehandling(sistIverksatteBehandlingId = behandlingId)
    }

    companion object {

        const val TASK_STEP_TYPE = "satsendring"

        fun opprettTask(behandlingsId: Long): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = behandlingsId.toString(),
                properties = Properties().apply {
                    this["behandlingsId"] = behandlingsId.toString()
                }
            )
        }
    }
}
