package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.beregning.endringstidspunkt.AktørId
import no.nav.familie.ba.sak.task.dto.OpprettVurderKonsekvensForYtelseOppgaveTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettVurderKonsekvensForYtelseOppgave.TASK_STEP_TYPE,
    beskrivelse = "Opprett oppgave i GOSYS for fødselshendelse som ikke lar seg utføre automatisk",
    maxAntallFeil = 3,
)
class OpprettVurderKonsekvensForYtelseOppgave(
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val opprettVurderKonsekvensForYtelseOppgaveTaskDTO = objectMapper.readValue(task.payload, OpprettVurderKonsekvensForYtelseOppgaveTaskDTO::class.java)
        task.metadata["oppgaveId"] = oppgaveService.opprettOppgaveForFødselshendelseUtenBehandling(
            ident = opprettVurderKonsekvensForYtelseOppgaveTaskDTO.ident,
            oppgavetype = opprettVurderKonsekvensForYtelseOppgaveTaskDTO.oppgavetype,
            fristForFerdigstillelse = opprettVurderKonsekvensForYtelseOppgaveTaskDTO.fristForFerdigstillelse,
            beskrivelse = opprettVurderKonsekvensForYtelseOppgaveTaskDTO.beskrivelse,
        )
    }

    companion object {

        const val TASK_STEP_TYPE = "opprettVurderKonsekvensForYtelseOppgave"

        fun opprettTask(
            ident: AktørId,
            oppgavetype: Oppgavetype,
            fristForFerdigstillelse: LocalDate,
            beskrivelse: String,
        ): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(
                    OpprettVurderKonsekvensForYtelseOppgaveTaskDTO(
                        ident,
                        oppgavetype,
                        fristForFerdigstillelse,
                        beskrivelse,
                    ),
                ),
            )
        }
    }
}
