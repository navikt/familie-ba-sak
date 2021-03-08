package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.ba.sak.task.dto.OpprettOppgaveTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@TaskStepBeskrivelse(taskStepType = OpprettOppgaveTask.TASK_STEP_TYPE,
                     beskrivelse = "Opprett oppgave i GOSYS for behandling",
                     maxAntallFeil = 3)
class OpprettOppgaveTask(
        private val oppgaveService: OppgaveService,
        private val taskRepository: TaskRepository) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val opprettOppgaveTaskDTO = objectMapper.readValue(task.payload, OpprettOppgaveTaskDTO::class.java)
        task.metadata["oppgaveId"] = oppgaveService.opprettOppgave(
                opprettOppgaveTaskDTO.behandlingId,
                opprettOppgaveTaskDTO.oppgavetype,
                opprettOppgaveTaskDTO.fristForFerdigstillelse,
                beskrivelse = opprettOppgaveTaskDTO.beskrivelse
        )
        taskRepository.saveAndFlush(task)
    }

    companion object {
        const val TASK_STEP_TYPE = "opprettOppgaveTask"

        fun opprettTask(behandlingId: Long, oppgavetype: Oppgavetype, fristForFerdigstillelse: LocalDate, beskrivelse: String? = null): Task {
            return Task.nyTask(
                    type = TASK_STEP_TYPE,
                    payload = objectMapper.writeValueAsString(OpprettOppgaveTaskDTO(behandlingId, oppgavetype, fristForFerdigstillelse, beskrivelse))
            )
        }
    }
}
