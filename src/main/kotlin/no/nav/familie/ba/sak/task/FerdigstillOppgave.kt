package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.arbeidsfordeling.OppgaveService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = FerdigstillOppgave.TASK_STEP_TYPE,
                     beskrivelse = "Ferdigstill oppgave i GOSYS for behandling",
                     maxAntallFeil = 3)
class FerdigstillOppgave(
        private val oppgaveService: OppgaveService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        oppgaveService.ferdigstillOppgave(behandlingsId = task.payload.toLong())
    }

    companion object {
        const val TASK_STEP_TYPE = "ferdigstillOppgaveTask"

        fun opprettTask(behandlingId: Long, metadata: Properties): Task {
            return Task.nyTask(type = TASK_STEP_TYPE,
                               payload = behandlingId.toString(),
                               properties = metadata
            )
        }
    }
}
