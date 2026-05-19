package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.task.OpprettTaskService.Companion.overstyrTaskMedNyCallId
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.log.IdUtils
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import tools.jackson.module.kotlin.readValue

@Service
@TaskStepBeskrivelse(
    taskStepType = LåsFagsakTask.TASK_STEP_TYPE,
    beskrivelse = "Lås fagsak og send melding til statistikk og Joark",
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
)
class LåsFagsakTask(
    private val fagsakService: FagsakService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val fagsakId = jsonMapper.readValue<Long>(task.payload)
        fagsakService.låsFagsak(fagsakId)
    }

    companion object {
        const val TASK_STEP_TYPE = "låsFagsakTask"

        fun opprettTask(fagsakId: Long): Task =
            overstyrTaskMedNyCallId(IdUtils.generateId()) {
                Task(
                    type = TASK_STEP_TYPE,
                    payload = jsonMapper.writeValueAsString(fagsakId),
                )
            }
    }
}
