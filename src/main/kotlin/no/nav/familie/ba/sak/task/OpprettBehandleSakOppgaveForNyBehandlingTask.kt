package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.arbeidsfordeling.OppgaveService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = OpprettBehandleSakOppgaveForNyBehandlingTask.TASK_STEP_TYPE,
                     beskrivelse = "Opprett BEH_SAK oppgave i GOSYS for nye behandlinger",
                     maxAntallFeil = 3)
class OpprettBehandleSakOppgaveForNyBehandlingTask(
        private val oppgaveService: OppgaveService,
        private val taskRepository: TaskRepository) : AsyncTaskStep {

    override fun doTask(task: Task) {
        task.metadata["oppgaveId"] = oppgaveService.opprettOppgaveForNyBehandling(task.payload.toLong())
        taskRepository.saveAndFlush(task)
    }

    companion object {
        const val TASK_STEP_TYPE = "opprettBehandleSakOppgaveForNyBehandlingTask"
    }
}
