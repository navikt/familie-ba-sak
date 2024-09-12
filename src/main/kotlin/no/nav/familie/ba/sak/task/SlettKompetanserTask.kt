package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.LocalDateProvider
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = SlettKompetanserTask.TASK_STEP_TYPE,
    beskrivelse = "Sletter alle kompetanse, utenlandske periodebeløp og valutakurser på en behandling som ikke er ferdig med behandlingsresultatsteget.",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = false,
)
class SlettKompetanserTask(
    private val kompetanseService: KompetanseService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val localDateProvider: LocalDateProvider,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = task.payload.toLong()

        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        if (!behandling.aktiv || behandling.status != BehandlingStatus.UTREDES || behandling.steg.rekkefølge > StegType.BEHANDLINGSRESULTAT.rekkefølge) {
            error("Behandling $behandlingId er gått forbi behandlingsresultatsteget og bør settes tilbake til et tidligere steg før man sletter kompetanse.")
        }
        slettKompetanserForBehandlingRekursivt(BehandlingId(behandlingId))
    }

    // Rekursivt siden sletting av kompetanse kan propagere ut og skape feil hvis vi ikke henter inn alle kompetanser på nytt hver gang vi sletter
    private fun slettKompetanserForBehandlingRekursivt(behandlingId: BehandlingId) {
        val kompetanser = kompetanseService.hentKompetanser(behandlingId)

        // autogeneres en tom kompetanse dersom alle er slettet
        if (kompetanser.size == 1 &&
            !kompetanser.first().erObligatoriskeFelterSatt() &&
            kompetanser.first().fom!! < localDateProvider.now().toYearMonth()
        ) {
            return
        }
        kompetanseService.slettKompetanse(behandlingId, kompetanser.first().id)
        slettKompetanserForBehandlingRekursivt(behandlingId)
    }

    companion object {
        const val TASK_STEP_TYPE = "SlettKompetanserTask"

        fun opprettTask(behandlingId: Long): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = behandlingId.toString(),
            )
    }
}
