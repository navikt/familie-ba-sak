package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.integrasjoner.økonomi.AvstemmingService
import no.nav.familie.ba.sak.task.dto.GrensesnittavstemmingTaskDTO
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.util.VirkedagerProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = GrensesnittavstemMotOppdrag.TASK_STEP_TYPE,
    beskrivelse = "Grensesnittavstemming mot oppdrag",
    // Rekjører kun 1 gang siden avstemming kan ha kjørt OK selv om tasken har feilet, så en autoretry kan trigge flere like grensesnittavstemminger
    maxAntallFeil = 1,
)
class GrensesnittavstemMotOppdrag(
    val avstemmingService: AvstemmingService,
    val opprettTaskService: OpprettTaskService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val avstemmingTask = jsonMapper.readValue(task.payload, GrensesnittavstemmingTaskDTO::class.java)
        logger.info("Gjør avstemming mot oppdrag fra og med ${avstemmingTask.fomDato} til og med ${avstemmingTask.tomDato}")

        avstemmingService.grensesnittavstemOppdrag(avstemmingTask.fomDato, avstemmingTask.tomDato, avstemmingTask.avstemmingId)
    }

    override fun onCompletion(task: Task) {
        opprettTaskService.opprettGrensesnittavstemMotOppdragTask(nesteAvstemmingDTO(task.triggerTid.toLocalDate()))
    }

    companion object {
        const val TASK_STEP_TYPE = "avstemMotOppdrag"

        fun nesteAvstemmingDTO(tideligereTriggerDato: LocalDate): GrensesnittavstemmingTaskDTO =
            GrensesnittavstemmingTaskDTO(
                tideligereTriggerDato.atStartOfDay(),
                VirkedagerProvider.nesteVirkedag(tideligereTriggerDato).atStartOfDay(),
                avstemmingId = UUID.randomUUID(),
            )

        private val logger: Logger = LoggerFactory.getLogger(GrensesnittavstemMotOppdrag::class.java)
    }
}
