package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.OpphørAvFullOvergangsstonadService
import no.nav.familie.ba.sak.task.dto.AutobrevOpphørOvergangsstonadDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = SendAutobrevOpphørOvergangsstonadTask.TASK_STEP_TYPE,
    beskrivelse = "Kjør automatisk behandling og send brev pga opphør av overgangsstonad",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = (60 * 60 * 24).toLong(),
    settTilManuellOppfølgning = true
)
class SendAutobrevOpphørOvergangsstonadTask(
    private val opphørAvFullOvergangsstonadService: OpphørAvFullOvergangsstonadService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val autobrevOpphørOvergangsstonadDTO =
            objectMapper.readValue(task.payload, AutobrevOpphørOvergangsstonadDTO::class.java)

        opphørAvFullOvergangsstonadService.opprettOmregningsoppgavePgaOpphørtOvergangsstønadInneværendeMåned(
            autobrevOpphørOvergangsstonadDTO
        )
    }

    companion object {

        const val TASK_STEP_TYPE = "SendAutobrevOpphørOvergangsstonadTask"
    }
}
