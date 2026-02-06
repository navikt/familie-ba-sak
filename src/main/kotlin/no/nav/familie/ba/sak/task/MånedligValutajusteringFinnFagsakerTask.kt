package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.YearMonth

@Service
@TaskStepBeskrivelse(
    taskStepType = MånedligValutajusteringFinnFagsakerTask.TASK_STEP_TYPE,
    beskrivelse = "Start månedlig valutajustering, finn alle fagsaker",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
)
class MånedligValutajusteringFinnFagsakerTask(
    val behandlingService: BehandlingHentOgPersisterService,
    val fagsakService: FagsakService,
    val kompetanseService: KompetanseService,
    val taskRepository: TaskRepositoryWrapper,
    val valutakursService: ValutakursService,
) : AsyncTaskStep {
    data class MånedligValutajusteringFinnFagsakerTaskDto(
        val måned: YearMonth,
    )

    @WithSpan
    override fun doTask(task: Task) {
        val data = jsonMapper.readValue(task.payload, MånedligValutajusteringFinnFagsakerTaskDto::class.java)

        logger.info("Starter månedlig valutajustering for ${data.måned}")

        val fagsakerMedLøpendeValutakurs = behandlingService.hentAlleFagsakerMedLøpendeValutakursIMåned(data.måned)

        fagsakerMedLøpendeValutakurs.forEach { taskRepository.save(MånedligValutajusteringTask.lagTask(it, data.måned)) }
    }

    companion object {
        const val TASK_STEP_TYPE = "månedligValutajusteringFinnFagsaker"
        private val logger = LoggerFactory.getLogger(MånedligValutajusteringFinnFagsakerTask::class.java)

        fun lagTask(
            inneværendeMåned: YearMonth,
            triggerTid: LocalDateTime,
        ) = Task(
            type = TASK_STEP_TYPE,
            payload = jsonMapper.writeValueAsString(MånedligValutajusteringFinnFagsakerTaskDto(inneværendeMåned)),
            mapOf(
                "måned" to inneværendeMåned.toString(),
                "callId" to (MDC.get(MDCConstants.MDC_CALL_ID) ?: IdUtils.generateId()),
            ).toProperties(),
        ).medTriggerTid(
            triggerTid = triggerTid,
        )
    }
}
