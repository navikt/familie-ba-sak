package no.nav.familie.ba.sak.task.dto

import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.AutovedtakSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@TaskStepBeskrivelse(
    taskStepType = HenleggAutovedtakOgSettBehandlingTilbakeTilVentTask.TASK_STEP_TYPE,
    beskrivelse = "Henlegg autovedtak og sett behandling tilbake til vent",
    maxAntallFeil = 1,
)
class HenleggAutovedtakOgSettBehandlingTilbakeTilVentTask(
    private val autovedtakSmåbarnstilleggService: AutovedtakSmåbarnstilleggService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) : AsyncTaskStep {
    @Transactional
    override fun doTask(task: Task) {
        val meldingIOppgave = "Småbarnstillegg: endring i overgangsstønad må behandles manuelt"
        val behandlingId = objectMapper.readValue(task.payload, Long::class.java)
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val metric =
            autovedtakSmåbarnstilleggService
                .antallVedtakOmOvergangsstønadTilManuellBehandling[AutovedtakSmåbarnstilleggService.TilManuellBehandlingÅrsak.NYE_UTBETALINGSPERIODER_FØRER_TIL_MANUELL_BEHANDLING]!!

        autovedtakSmåbarnstilleggService.kanIkkeBehandleAutomatisk(behandling, metric, meldingIOppgave)
    }

    companion object {
        const val TASK_STEP_TYPE = "HenleggAutovedtakOgSettBehandlingTilbakeTilVentTask"

        fun opprettTask(behandlingId: Long) =
            Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(behandlingId),
                properties = mapOf("behandlingId" to behandlingId.toString()).toProperties(),
            )
    }
}
