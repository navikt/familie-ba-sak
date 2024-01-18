package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.internal.ForvalterService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = PatchFomPåVilkårTilFødselsdatoTask.TASK_STEP_TYPE,
    beskrivelse = "Patcher fom på vilkår til å bli satt til fødselsdato dersom fom er før fødselsdato.",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
)
class PatchFomPåVilkårTilFødselsdatoTask(
    private val forvalterService: ForvalterService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val dto = objectMapper.readValue(task.payload, PatchFomPåVilkårTilFødselsdato::class.java)

        dto.behandlinger.forEach {
            forvalterService.settFomPåVilkårTilPersonsFødselsdato(it)
        }
    }

    companion object {
        const val TASK_STEP_TYPE = "PatchFomPaaVilkaar"
    }
}

data class PatchFomPåVilkårTilFødselsdato(
    val behandlinger: Set<Long>,
)
