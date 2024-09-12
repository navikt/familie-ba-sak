package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
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
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = task.payload.toLong()

        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        if (!behandling.aktiv || behandling.status != BehandlingStatus.UTREDES) {
            error("Behandling $behandlingId er ikke i ferd med å utredes.")
        }
        kompetanseService.skjemaService.slettSkjemaer(BehandlingId(behandlingId))
        tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(behandling)
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
