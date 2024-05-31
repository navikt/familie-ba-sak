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
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.erUtdaterteValutakurserIMåned
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

        val sisteEøsBehanldingerIFagsakerMedEøsBehandlinger = behandlingService.hentSisteIverksatteEØSBehandlingFraLøpendeFagsaker().toSet().sorted()

        // Hardkoder denne til å kun ta 10 behanldinger i første omgang slik at vi er helt sikre på at vi ikke kjører på alle behandlinger mens vi tester.
        sisteEøsBehanldingerIFagsakerMedEøsBehandlinger.take(10).forEach { behandlingid ->
            val valutakurser = valutakursService.hentValutakurser(BehandlingId(behandlingid))

            if (valutakurser.erUtdaterteValutakurserIMåned(data.måned)) {
                taskRepository.save(MånedligValutajusteringTask.lagTask(behandlingid, data.måned))
            }
        }
    }

    companion object {
        const val TASK_STEP_TYPE = "månedligValutajusteringFinnFagsaker"
        private val logger = LoggerFactory.getLogger(MånedligValutajusteringFinnFagsakerTask::class.java)

        fun lagTask(inneværendeMåned: YearMonth) =
            Task(
                type = MånedligValutajusteringFinnFagsakerTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(MånedligValutajusteringFinnFagsakerTaskDto(inneværendeMåned)),
                mapOf("måned" to inneværendeMåned.toString()).toProperties(),
            )

        fun erSekundærlandIMåned(
            kompetanser: Collection<Kompetanse>,
            yearMonth: YearMonth,
        ) = kompetanser.filter { (it.fom ?: TIDENES_MORGEN.toYearMonth()).isSameOrBefore(yearMonth) && (it.tom ?: TIDENES_ENDE.toYearMonth()).isSameOrAfter(yearMonth) }
            .any { kompetanse -> kompetanse.resultat == KompetanseResultat.NORGE_ER_SEKUNDÆRLAND }
    }
}
