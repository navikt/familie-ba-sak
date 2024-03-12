package no.nav.familie.ba.sak.task

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
import java.time.LocalDate
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
class MånedligValutajusteringFinnFagsakerTask(val behandlingService: BehandlingHentOgPersisterService, val fagsakService: FagsakService, val kompetanseService: KompetanseService, val taskService: OpprettTaskService) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val valutajusteringsMåned =
            objectMapper.readValue(task.payload, LocalDate::class.java)

        val relevanteBehandlinger = behandlingService.hentSisteIverksatteEØSBehandlingFraLøpendeFagsaker().toSet().sorted()
        relevanteBehandlinger.forEach { behandlingid ->
            // check if behandling is eøs sekundærland
            val ersekundærland = kompetanseService.hentKompetanser(BehandlingId(behandlingid)).any { kompetanse -> kompetanse.resultat == KompetanseResultat.NORGE_ER_SEKUNDÆRLAND }
            if (ersekundærland) {
                lagMånedligValutajusteringTask(behandlingid, valutajusteringsMåned)
            }
        }
    }

    private fun lagMånedligValutajusteringTask(
        behandlingId: Long,
        valutajusteringsMåned: LocalDate,
    ) {
        taskService.opprettMånedligValutajusteringTask(behandlingId, YearMonth.from(valutajusteringsMåned))
    }

    companion object {
        const val TASK_STEP_TYPE = "månedligvalutajuteringfinnfagsaker"
        private val logger = LoggerFactory.getLogger(MånedligValutajusteringFinnFagsakerTask::class.java)
    }
}
