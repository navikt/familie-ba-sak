package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.task.dto.OpprettVurderFødselshendelseKonsekvensForYtelseOppgaveTaskDTO
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettVurderFødselshendelseKonsekvensForYtelseOppgave.TASK_STEP_TYPE,
    beskrivelse = "Opprett oppgave i GOSYS for fødselshendelse som ikke lar seg utføre automatisk",
    maxAntallFeil = 3,
)
class OpprettVurderFødselshendelseKonsekvensForYtelseOppgave(
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val opprettVurderFødselshendelseKonsekvensForYtelseOppgaveTaskDTO = jsonMapper.readValue(task.payload, OpprettVurderFødselshendelseKonsekvensForYtelseOppgaveTaskDTO::class.java)
        task.metadata["oppgaveId"] =
            oppgaveService.opprettOppgaveForFødselshendelse(
                aktørId = opprettVurderFødselshendelseKonsekvensForYtelseOppgaveTaskDTO.ident,
                oppgavetype = opprettVurderFødselshendelseKonsekvensForYtelseOppgaveTaskDTO.oppgavetype,
                fristForFerdigstillelse = LocalDate.now(),
                beskrivelse = opprettVurderFødselshendelseKonsekvensForYtelseOppgaveTaskDTO.beskrivelse,
            )
    }

    companion object {
        const val TASK_STEP_TYPE = "opprettVurderFødselshendelseKonsekvensForYtelseOppgave"

        fun opprettTask(
            aktør: Aktør,
            oppgavetype: Oppgavetype,
            beskrivelse: String,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload =
                    jsonMapper.writeValueAsString(
                        OpprettVurderFødselshendelseKonsekvensForYtelseOppgaveTaskDTO(
                            aktør.aktørId,
                            oppgavetype,
                            beskrivelse,
                        ),
                    ),
            )
    }
}
