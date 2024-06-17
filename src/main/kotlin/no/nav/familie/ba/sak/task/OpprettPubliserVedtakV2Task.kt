package no.nav.familie.ba.sak.task

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.statistikk.stønadsstatistikk.StønadsstatistikkService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.system.measureTimeMillis

@Service
@TaskStepBeskrivelse(taskStepType = OpprettPubliserVedtakV2Task.TASK_STEP_TYPE, beskrivelse = "Oppretter Publiser vedtak V2 tasker", maxAntallFeil = 1)
class OpprettPubliserVedtakV2Task(
    private val stønadsstatistikkService: StønadsstatistikkService,
    private val taskRepository: TaskRepositoryWrapper,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlinger = objectMapper.readValue<Set<Long>>(task.payload)

        val tidBrukt =
            measureTimeMillis {
                behandlinger.forEach {
                    val vedtakV2DVH = stønadsstatistikkService.hentVedtakV2(it)
                    val vedtakV2Task = PubliserVedtakV2Task.opprettTask(vedtakV2DVH.personV2.personIdent, it)
                    taskRepository.save(vedtakV2Task)
                }
            }

        logger.info(
            "Fullført kjøring av OpprettPubliserVedtakV2Task for ${behandlinger.size}. " +
                "Tid brukt = $tidBrukt millisekunder",
        )
    }

    companion object {
        const val TASK_STEP_TYPE = "opprettPubliserVedtakV2Task"
        val logger = LoggerFactory.getLogger(OpprettPubliserVedtakV2Task::class.java)

        fun opprettTask(
            behandlinger: Set<Long>,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(behandlinger),
            )
    }
}
