package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.task.dto.OpprettVurderLivshendelseOppgaveForFinnmarksOgSvalbardtilleggTaskDTO
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettVurderLivshendelseOppgaveForFinnmarksOgSvalbardtilleggTask.TASK_STEP_TYPE,
    beskrivelse = "Opprett oppgave i GOSYS for Finnmarks- og Svalbardtilleggbehandlinger som ikke lar seg utføre automatisk",
    maxAntallFeil = 3,
)
class OpprettVurderLivshendelseOppgaveForFinnmarksOgSvalbardtilleggTask(
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val taskDTO = jsonMapper.readValue(task.payload, OpprettVurderLivshendelseOppgaveForFinnmarksOgSvalbardtilleggTaskDTO::class.java)
        task.metadata["oppgaveId"] =
            oppgaveService.opprettOppgaveForFinnmarksOgSvalbardtillegg(
                fagsakId = taskDTO.fagsakId,
                beskrivelse = taskDTO.beskrivelse,
            )
    }

    companion object {
        const val TASK_STEP_TYPE = "opprettVurderLivshendelseOppgaveForFinnmarksOgSvalbardtilleggTask"

        fun opprettTask(
            fagsakId: Long,
            beskrivelse: String,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload =
                    jsonMapper.writeValueAsString(
                        OpprettVurderLivshendelseOppgaveForFinnmarksOgSvalbardtilleggTaskDTO(
                            fagsakId,
                            beskrivelse,
                        ),
                    ),
            )
    }
}
