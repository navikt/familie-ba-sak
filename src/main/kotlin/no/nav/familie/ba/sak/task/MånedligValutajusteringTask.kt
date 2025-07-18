package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.AutovedtakMånedligValutajusteringService
import no.nav.familie.ba.sak.task.OpprettTaskService.Companion.overstyrTaskMedNyCallId
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
@TaskStepBeskrivelse(
    taskStepType = MånedligValutajusteringTask.TASK_STEP_TYPE,
    beskrivelse = "månedlig valutajustering",
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
)
class MånedligValutajusteringTask(
    val autovedtakMånedligValutajusteringService: AutovedtakMånedligValutajusteringService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val taskdto = objectMapper.readValue(task.payload, MånedligValutajusteringTaskDto::class.java)
        autovedtakMånedligValutajusteringService.utførMånedligValutajustering(
            fagsakId = taskdto.fagsakId,
            måned = taskdto.måned,
        )
    }

    data class MånedligValutajusteringTaskDto(
        val fagsakId: Long,
        val måned: YearMonth,
    )

    companion object {
        const val TASK_STEP_TYPE = "månedligValutajustering"

        fun lagTask(
            fagsakId: Long,
            valutajusteringsMåned: YearMonth,
        ): Task =
            overstyrTaskMedNyCallId(IdUtils.generateId()) {
                Task(
                    type = TASK_STEP_TYPE,
                    payload = objectMapper.writeValueAsString(MånedligValutajusteringTaskDto(fagsakId = fagsakId, måned = valutajusteringsMåned)),
                    properties =
                        mapOf(
                            "fagsakId" to fagsakId.toString(),
                            "måned" to valutajusteringsMåned.toString(),
                            "callId" to (MDC.get(MDCConstants.MDC_CALL_ID) ?: IdUtils.generateId()),
                        ).toProperties(),
                )
            }
    }
}
