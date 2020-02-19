package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = FerdigstillOppgave.TASK_STEP_TYPE, beskrivelse = "Opprett BEH_SAK oppgave i GOSYS for nye behandlinger", maxAntallFeil = 3)
class FerdigstillOppgave(
        private val oppgaveService: OppgaveService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val payload = objectMapper.readValue(task.payload, IverksettingTaskDTO::class.java)

        oppgaveService.ferdigstillOppgave(payload.behandlingsId)

    }

    companion object {
        const val TASK_STEP_TYPE = "ferdigstillOppgaveTask"
    }
}
