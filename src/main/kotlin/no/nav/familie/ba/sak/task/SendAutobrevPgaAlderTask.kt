package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.AutobrevOmregningPgaAlderService
import no.nav.familie.ba.sak.task.dto.AutobrevPgaAlderDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth

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
    private val logger = LoggerFactory.getLogger(SendAutobrevPgaAlderTask::class.java)

    @WithSpan
    override fun doTask(task: Task) {
        val autobrevDTO = objectMapper.readValue(task.payload, AutobrevPgaAlderDTO::class.java)

        if (!YearMonth.now().equals(autobrevDTO.årMåned)) {
            logger.info("Task for autobrev må kjøres innenfor måneden det skal sjekkes mot.")
            task.metadata["resultat"] = "Ignorerer task, da den ikke kjøres i riktig måned"
        } else {
            task.metadata["resultat"] =
                autobrevOmregningPgaAlderService.opprettOmregningsoppgaveForBarnIBrytingsalder(autobrevDTO, task.opprettetTid).name
        }
    }

    companion object {
        const val TASK_STEP_TYPE = "sendAutobrevPgaAlderTask"
    }
}
