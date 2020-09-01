package no.nav.familie.ba.sak.task

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.ba.sak.task.dto.OpprettOppgaveDTO
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


    private val antallOppgaveTyper: Map<Oppgavetype, Counter> = Oppgavetype.values().map {
        it to Metrics.counter("oppgave.opprettet", "type", it.name)
    }.toMap()

    override fun doTask(task: Task) {
        val opprettOppgaveDTO = objectMapper.readValue(task.payload, OpprettOppgaveDTO::class.java)
        task.metadata["oppgaveId"] = oppgaveService.opprettOppgave(
                opprettOppgaveDTO.behandlingId,
                opprettOppgaveDTO.oppgavetype,
                opprettOppgaveDTO.fristForFerdigstillelse
        )
        taskRepository.saveAndFlush(task)
        antallOppgaveTyper[opprettOppgaveDTO.oppgavetype]?.increment()
    }

    companion object {
        const val TASK_STEP_TYPE = "opprettOppgaveTask"

        fun opprettTask(behandlingId: Long, oppgavetype: Oppgavetype, fristForFerdigstillelse: LocalDate): Task {
            return Task.nyTask(
                    type = TASK_STEP_TYPE,
                    payload = objectMapper.writeValueAsString(OpprettOppgaveDTO(behandlingId,
                                                                                oppgavetype,
                                                                                fristForFerdigstillelse))
            )
        }
    }
}
