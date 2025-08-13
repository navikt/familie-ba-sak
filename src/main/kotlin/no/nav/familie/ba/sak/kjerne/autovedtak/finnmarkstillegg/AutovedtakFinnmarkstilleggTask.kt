package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = AutovedtakFinnmarkstilleggTask.TASK_STEP_TYPE,
    beskrivelse = "Gjennomfør autovedtak for Finnmarkstillegg",
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
)
class AutovedtakFinnmarkstilleggTask(
    private val autovedtakStegService: AutovedtakStegService,
    private val fagsakService: FagsakService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val fagsakId = task.payload.toLong()
        val aktør = fagsakService.hentAktør(fagsakId)
        autovedtakStegService.kjørBehandlingFinnmarkstillegg(
            mottakersAktør = aktør,
            fagsakId = fagsakId,
            førstegangKjørt = task.opprettetTid,
        )
    }

    companion object {
        const val TASK_STEP_TYPE = "autovedtakFinnmarkstilleggTask"
    }
}
