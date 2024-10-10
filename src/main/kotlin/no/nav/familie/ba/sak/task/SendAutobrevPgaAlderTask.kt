package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.AutobrevOmregningPgaAlderService
import no.nav.familie.ba.sak.task.dto.AutobrevPgaAlderDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@TaskStepBeskrivelse(
    taskStepType = SendAutobrevPgaAlderTask.TASK_STEP_TYPE,
    beskrivelse = "Send autobrev som trigges pga alder til Dokdist. F.eks. når barn blir 18 år",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = (60 * 60 * 24).toLong(),
    settTilManuellOppfølgning = true,
)
class SendAutobrevPgaAlderTask(
    private val autobrevOmregningPgaAlderService: AutobrevOmregningPgaAlderService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val autobrevDTO = objectMapper.readValue(task.payload, AutobrevPgaAlderDTO::class.java)

        if (!LocalDate.now().toYearMonth().equals(autobrevDTO.årMåned)) {
            throw Feil("Task for autobrev må kjøres innenfor måneden det skal sjekkes mot.")
        }

        task.metadata["resultat"] =
            autobrevOmregningPgaAlderService.opprettOmregningsoppgaveForBarnIBrytingsalder(autobrevDTO, task.opprettetTid).name
    }

    companion object {
        const val TASK_STEP_TYPE = "sendAutobrevPgaAlderTask"
    }
}
