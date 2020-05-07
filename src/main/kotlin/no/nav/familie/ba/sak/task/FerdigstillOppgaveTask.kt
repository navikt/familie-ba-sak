package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.task.dto.FerdigstillOppgaveDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = FerdigstillOppgaveTask.TASK_STEP_TYPE,
                     beskrivelse = "Ferdigstill oppgave i GOSYS for behandling",
                     maxAntallFeil = 3)
class FerdigstillOppgaveTask(
        private val stegService: StegService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val ferdigstillOppgave = objectMapper.readValue(task.payload, FerdigstillOppgaveDTO::class.java)

        stegService.håndterFerdigstillBehandleSakOppgave(ferdigstillOppgave = ferdigstillOppgave)
    }

    companion object {
        const val TASK_STEP_TYPE = "ferdigstillOppgaveTask"

        fun opprettTask(behandlingId: Long, oppgavetype: Oppgavetype): Task {
            return Task.nyTask(type = TASK_STEP_TYPE,
                               payload = objectMapper.writeValueAsString(FerdigstillOppgaveDTO(behandlingId = behandlingId,
                                                                                               oppgavetype = oppgavetype))
            )
        }
    }
}
