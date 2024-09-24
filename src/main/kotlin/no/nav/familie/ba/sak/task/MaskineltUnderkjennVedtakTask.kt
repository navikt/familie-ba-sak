package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = MaskineltUnderkjennVedtakTask.TASK_STEP_TYPE,
    beskrivelse = "Underkjenner et vedtak på vegne av system",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = false,
)
class MaskineltUnderkjennVedtakTask(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val stegService: StegService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = task.payload.toLong()

        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        stegService.håndterBeslutningForVedtak(
            behandling = behandling,
            restBeslutningPåVedtak =
                RestBeslutningPåVedtak(
                    beslutning = Beslutning.UNDERKJENT,
                    begrunnelse = "Maskinelt underkjent",
                ),
        )
    }

    companion object {
        const val TASK_STEP_TYPE = "maskineltUnderkjennVedtakTask"

        fun opprettTask(behandlingId: Long): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = behandlingId.toString(),
            )
    }
}
