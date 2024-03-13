package no.nav.familie.ba.sak.task

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
@TaskStepBeskrivelse(
    taskStepType =
        MånedligValutajusteringTask
            .TASK_STEP_TYPE,
    beskrivelse = "månedlig valutajustering",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
)
class MånedligValutajusteringTask() : AsyncTaskStep {
    override fun doTask(task: Task) {
        val taskdto = objectMapper.readValue(task.payload, MånedligValutajusteringTaskDto::class.java)
        logger.info("Starter Task månedlig valutajustering for $taskdto")
        logger.info("Månedlig valutajustering er ikke implementert enda. Logger kun dette enn så lenge.")
        // autovedtakMånedligValutajusteringService.utførMånedligValutajustering(taskdto)
    }

    data class MånedligValutajusteringTaskDto(
        val behandlingid: Long,
        val måned: YearMonth,
    )

    companion object {
        const val TASK_STEP_TYPE = "månedligValutajustering"
        private val logger = LoggerFactory.getLogger(MånedligValutajusteringTask::class.java)

        fun lagTask(
            behandlingId: Long,
            valutajusteringsMåned: YearMonth,
        ): Task =
            Task(
                type = MånedligValutajusteringTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(MånedligValutajusteringTaskDto(behandlingid = behandlingId, måned = valutajusteringsMåned)),
                properties =
                    mapOf(
                        "behandlingId" to behandlingId.toString(),
                        "måned" to valutajusteringsMåned.toString(),
                    ).toProperties(),
            )
    }
}
