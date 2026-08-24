package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.module.kotlin.readValue

@Service
@TaskStepBeskrivelse(
    taskStepType = FinnFagsakerSomSkalLåsesTask.TASK_STEP_TYPE,
    beskrivelse = "Finn fagsaker som skal låses",
    maxAntallFeil = 3,
)
class FinnFagsakerSomSkalLåsesTask(
    private val taskRepository: TaskRepositoryWrapper,
    private val fagsakRepository: FagsakRepository,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val maksAntall = jsonMapper.readValue<Int>(task.payload)
        fagsakRepository
            .finnAvsluttedeFagsakerSomSkalLåses(maksAntall = maksAntall)
            .also { logger.info("Fant ${it.size} fagsaker som skal låses (maks $maksAntall)") }
            .forEach { taskRepository.save(LåsFagsakTask.opprettTask(it)) }
    }

    companion object {
        const val TASK_STEP_TYPE = "finnFagsakerSomSkalLåsesTask"
        private val logger = LoggerFactory.getLogger(FinnFagsakerSomSkalLåsesTask::class.java)

        fun opprettTask(maksAntall: Int): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = jsonMapper.writeValueAsString(maksAntall),
            )
    }
}
