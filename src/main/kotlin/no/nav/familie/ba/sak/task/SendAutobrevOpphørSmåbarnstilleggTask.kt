package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.AutobrevOpphørSmåbarnstilleggService
import no.nav.familie.ba.sak.task.dto.AutobrevOpphørSmåbarnstilleggDTO
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = SendAutobrevOpphørSmåbarnstilleggTask.TASK_STEP_TYPE,
    beskrivelse = "Send autobrev for opphør av småbarnstillegg",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = (60 * 60 * 24).toLong(),
    settTilManuellOppfølgning = true,
)
class SendAutobrevOpphørSmåbarnstilleggTask(
    private val autobrevOpphørSmåbarnstilleggService: AutobrevOpphørSmåbarnstilleggService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val autobrevDTO = jsonMapper.readValue(task.payload, AutobrevOpphørSmåbarnstilleggDTO::class.java)

        autobrevOpphørSmåbarnstilleggService.kjørBehandlingOgSendBrevForOpphørAvSmåbarnstillegg(
            fagsakId = autobrevDTO.fagsakId,
            førstegangKjørt = task.opprettetTid,
        )
    }

    companion object {
        const val TASK_STEP_TYPE = "sendAutobrevOpphorSmaabarnstillegg"
    }
}
