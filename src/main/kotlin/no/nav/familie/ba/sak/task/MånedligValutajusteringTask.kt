package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.AutovedtakMånedligValutajusteringService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.erAlleValutakurserOppdaterteIMåned
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
class MånedligValutajusteringTask(
    val autovedtakMånedligValutajusteringService: AutovedtakMånedligValutajusteringService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val valutakursService: ValutakursService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val taskdto = objectMapper.readValue(task.payload, MånedligValutajusteringTaskDto::class.java)
        logger.info("Starter Task månedlig valutajustering for $taskdto")

        val behandling = behandlingHentOgPersisterService.hent(taskdto.behandlingid)
        val sisteVedtatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)
                ?: throw Feil("Fant ingen vedtatte behandlinger for fagsak med id ${behandling.fagsak.id}")

        if (taskdto.behandlingid != sisteVedtatteBehandling.id) {
            val sisteValutakurser = valutakursService.hentValutakurser(BehandlingId(sisteVedtatteBehandling.id))
            if (sisteValutakurser.erAlleValutakurserOppdaterteIMåned(taskdto.måned)) {
                logger.info("Valutakursene er allerede oppdatert for fagsak ${behandling.fagsak.id}. Hopper ut")
                return
            }
        }

        autovedtakMånedligValutajusteringService.utførMånedligValutajustering(
            behandlingid = taskdto.behandlingid,
            måned = taskdto.måned,
        )
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
