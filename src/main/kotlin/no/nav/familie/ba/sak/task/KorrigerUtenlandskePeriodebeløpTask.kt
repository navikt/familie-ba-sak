package no.nav.familie.ba.sak.task

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.internal.ForvalterService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = KorrigerUtenlandskePeriodebeløpTask.TASK_STEP_TYPE,
    beskrivelse = "Korriger utenlandske periodebeløp med feil",
    maxAntallFeil = 1,
)
class KorrigerUtenlandskePeriodebeløpTask(val forvalterService: ForvalterService) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlinger: List<Long> = objectMapper.readValue(task.payload)
        forvalterService.korrigerUtenlandskePeriodebeløp(behandlinger)
    }

    companion object {
        const val TASK_STEP_TYPE = "korrigerUtenlandskePeriodebeløp"
    }
}
