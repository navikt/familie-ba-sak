package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.erAlleValutakurserOppdaterteIMåned
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.kontrakter.felles.objectMapper
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

    override fun doTask(task: Task) {
        val data = objectMapper.readValue(task.payload, MånedligValutajusteringFinnFagsakerTaskDto::class.java)

        logger.info("Starter månedlig valutajustering for ${data.måned}")

        val fagsakerMedLøpendeValutakurs = behandlingService.hentAlleFagsakerMedLøpendeValutakursIMåned(data.måned)

        // Hardkoder denne til å kun ta 10 behanldinger i første omgang slik at vi er helt sikre på at vi ikke kjører på alle behandlinger mens vi tester.
        fagsakerMedLøpendeValutakurs.take(10).forEach { fagsakId ->
            val sisteVedtatteBehandling = behandlingService.hentSisteBehandlingSomErVedtatt(fagsakId) ?: throw Feil("Fant ikke siste vedtatte behandling for $fagsakId")
            val valutakurser = valutakursService.hentValutakurser(BehandlingId(sisteVedtatteBehandling.id))

            if (!valutakurser.erAlleValutakurserOppdaterteIMåned(data.måned)) {
                taskRepository.save(MånedligValutajusteringTask.lagTask(fagsakId, data.måned))
            }
        }
    }

    companion object {
        const val TASK_STEP_TYPE = "månedligValutajusteringFinnFagsaker"
        private val logger = LoggerFactory.getLogger(MånedligValutajusteringFinnFagsakerTask::class.java)

        fun lagTask(
            inneværendeMåned: YearMonth,
            triggerTid: LocalDateTime,
        ) =
            Task(
                type = MånedligValutajusteringFinnFagsakerTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(MånedligValutajusteringFinnFagsakerTaskDto(inneværendeMåned)),
                mapOf(
                    "måned" to inneværendeMåned.toString(),
                    "callId" to (MDC.get(MDCConstants.MDC_CALL_ID) ?: IdUtils.generateId()),
                ).toProperties(),
            ).medTriggerTid(
                triggerTid = triggerTid,
            )
    }
}
