package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
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
        MånedligValutajusteringFinnFagsakerTask
            .TASK_STEP_TYPE,
    beskrivelse = "Start månedlig valutajustering, finn alle fagsaker",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
)
class MånedligValutajusteringFinnFagsakerTask(
    val behandlingService: BehandlingHentOgPersisterService,
    val fagsakService: FagsakService,
    val kompetanseService: KompetanseService,
    val taskRepository: TaskRepositoryWrapper,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val data =
            objectMapper.readValue(task.payload, MånedligValutajusteringFinnFagsakerTaskDto::class.java)

        val relevanteBehandlinger = behandlingService.hentSisteIverksatteEØSBehandlingFraLøpendeFagsaker().toSet().sorted()
        relevanteBehandlinger.forEach { behandlingid ->
            // check if behandling is eøs sekundærland
            val ersekundærland =
                kompetanseService.hentKompetanser(BehandlingId(behandlingid))
                    .filter { (it.fom ?: TIDENES_MORGEN.toYearMonth()).isSameOrBefore(data.måned) && (it.tom ?: TIDENES_ENDE.toYearMonth()).isSameOrAfter(data.måned) }
                    .any { kompetanse -> kompetanse.resultat == KompetanseResultat.NORGE_ER_SEKUNDÆRLAND }

            if (ersekundærland) {
                lagMånedligValutajusteringTask(behandlingid, data.måned)
            }
        }
    }

    private fun lagMånedligValutajusteringTask(
        behandlingId: Long,
        valutajusteringsMåned: YearMonth,
    ) {
        taskRepository.save(
            Task(
                type = MånedligValutajusteringTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(MånedligValutajusteringTaskDto(behandlingid = behandlingId, måned = valutajusteringsMåned)),
                properties =
                    mapOf(
                        "behandlingId" to behandlingId.toString(),
                        "måned" to valutajusteringsMåned.toString(),
                    ).toProperties(),
            ),
        )
    }

    data class MånedligValutajusteringFinnFagsakerTaskDto(
        val måned: YearMonth,
    )

    companion object {
        const val TASK_STEP_TYPE = "månedligvalutajuteringfinnfagsaker"
        private val logger = LoggerFactory.getLogger(MånedligValutajusteringFinnFagsakerTask::class.java)

        fun lagTask(inneværendeMåned: YearMonth) =
            Task(
                type = MånedligValutajusteringFinnFagsakerTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(MånedligValutajusteringFinnFagsakerTaskDto(inneværendeMåned)),
            )
    }
}
