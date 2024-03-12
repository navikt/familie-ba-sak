package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.AutovedtakMånedligValutajusteringService
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
class MånedligValutajusteringTask(private val autovedtakMånedligValutajusteringService: AutovedtakMånedligValutajusteringService) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val taskdto = objectMapper.readValue(task.payload, MånedligValutajusteringTaskDto::class.java)
        logger.info("Starter Task månedlig valutajustering for $taskdto")
        autovedtakMånedligValutajusteringService.utførMånedligValutajustering(taskdto)
    }

    companion object {
        const val TASK_STEP_TYPE = "månedligvalutajutering"
        private val logger = LoggerFactory.getLogger(MånedligValutajusteringTask::class.java)
    }
}

data class MånedligValutajusteringTaskDto(
    val behandlingid: Long,
    val måned: YearMonth,
)
