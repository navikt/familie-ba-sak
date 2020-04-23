package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
        taskStepType = OpprettGodkjenneVedtakOppgaveTilBeslutter.TASK_STEP_TYPE,
        beskrivelse = "Opprett Godkjenne Vedtak-oppgave for behandling som er sendt til beslutter",
        maxAntallFeil = 3
)
class OpprettGodkjenneVedtakOppgaveTilBeslutter(private val oppgaveService: OppgaveService,
                                                private val taskRepository: TaskRepository): AsyncTaskStep {
    override fun doTask(task: Task) {
        task.metadata["oppgaveId"] = oppgaveService.opprettOppgaveForGodkjenneVedtak(task.payload.toLong())
        taskRepository.saveAndFlush(task)
    }

    companion object {
        const val TASK_STEP_TYPE = "opprettGodkjenneVedtakOppgaveTilBeslutter"
    }
}