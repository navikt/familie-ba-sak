package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = OpprettOppgaveTask.TASK_STEP_TYPE, beskrivelse = "Opprett oppgave i GOSYS", maxAntallFeil = 3)
class OpprettOppgaveTask(
        private val oppgaveService: OppgaveService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        oppgaveService.opprettOppgaveForNyBehandling(task.payload.toLong())
    }

    companion object {
        const val TASK_STEP_TYPE = "opprettOppgaveTask"
    }
}
