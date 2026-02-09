package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.task.dto.FerdigstillBehandlingDTO
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = FerdigstillBehandlingTask.TASK_STEP_TYPE,
    beskrivelse = "Ferdigstill behandling",
    maxAntallFeil = 3,
)
class FerdigstillBehandlingTask(
    val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    val stegService: StegService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val ferdigstillBehandling = jsonMapper.readValue(task.payload, FerdigstillBehandlingDTO::class.java)
        stegService.håndterFerdigstillBehandling(
            behandling =
                behandlingHentOgPersisterService.hent(
                    ferdigstillBehandling.behandlingsId,
                ),
        )
    }

    companion object {
        const val TASK_STEP_TYPE = "ferdigstillBehandling"

        fun opprettTask(
            søkerIdent: String,
            behandlingsId: Long,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload =
                    jsonMapper.writeValueAsString(
                        FerdigstillBehandlingDTO(
                            personIdent = søkerIdent,
                            behandlingsId = behandlingsId,
                        ),
                    ),
                properties =
                    Properties().apply {
                        this["personIdent"] = søkerIdent
                        this["behandlingId"] = behandlingsId.toString()
                    },
            )
    }
}
