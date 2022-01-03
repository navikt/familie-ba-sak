package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.SatsendringService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = StartSatsendringForAlleBehandlingerTask.TASK_STEP_TYPE,
    beskrivelse = "Utfør satsendring for alle behandlinger",
    maxAntallFeil = 1
)
class StartSatsendringForAlleBehandlingerTask(
    val satsendringService: SatsendringService,
    val taskRepository: TaskRepositoryWrapper
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val gammelSats = task.payload.toLong()
        satsendringService.finnBehandlingerForSatsendring(gammelSats, YearMonth.now()).forEach {
            taskRepository.save(SatsendringTask.opprettTask(it))
        }
    }

    companion object {

        const val TASK_STEP_TYPE = "startsatsendringforallebehandlinger"

        fun opprettTask(gammelSats: Long): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = gammelSats.toString(),
                properties = Properties().apply {
                    this["måned"] = YearMonth.now()
                    this["gammelSats"] = gammelSats.toString()
                }
            )
        }
    }
}
