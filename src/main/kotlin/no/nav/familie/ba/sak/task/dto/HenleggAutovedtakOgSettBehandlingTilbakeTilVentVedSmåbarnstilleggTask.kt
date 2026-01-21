package no.nav.familie.ba.sak.task.dto

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.AutovedtakSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = HenleggAutovedtakOgSettBehandlingTilbakeTilVentVedSmåbarnstilleggTask.TASK_STEP_TYPE,
    beskrivelse = "Henlegg autovedtak og sett behandling tilbake til vent",
    maxAntallFeil = 1,
)
class HenleggAutovedtakOgSettBehandlingTilbakeTilVentVedSmåbarnstilleggTask(
    private val autovedtakSmåbarnstilleggService: AutovedtakSmåbarnstilleggService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val meldingIOppgave = "Småbarnstillegg: endring i overgangsstønad må behandles manuelt"
        val behandlingId = jsonMapper.readValue(task.payload, Long::class.java)
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val metric =
            autovedtakSmåbarnstilleggService
                .antallVedtakOmOvergangsstønadTilManuellBehandling[AutovedtakSmåbarnstilleggService.TilManuellBehandlingÅrsak.NYE_UTBETALINGSPERIODER_FØRER_TIL_MANUELL_BEHANDLING]!!

        autovedtakSmåbarnstilleggService.kanIkkeBehandleAutomatisk(behandling, metric, meldingIOppgave)
    }

    companion object {
        const val TASK_STEP_TYPE = "henleggAutovedtakOgSettBehandlingTilbakeTilVentTask"

        fun opprettTask(behandlingId: Long) =
            Task(
                type = TASK_STEP_TYPE,
                payload = jsonMapper.writeValueAsString(behandlingId),
                properties = mapOf("behandlingId" to behandlingId.toString()).toProperties(),
            )
    }
}
