package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.integrasjoner.økonomi.AvstemmingService
import no.nav.familie.ba.sak.integrasjoner.økonomi.BatchService
import no.nav.familie.ba.sak.integrasjoner.økonomi.DataChunkRepository
import no.nav.familie.ba.sak.integrasjoner.økonomi.KjøreStatus
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingAvsluttTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.RekjørSenereException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@TaskStepBeskrivelse(
    taskStepType = KonsistensavstemMotOppdragAvsluttTask.TASK_STEP_TYPE,
    beskrivelse = "Avslutt Konsistensavstemming mot oppdrag",
    maxAntallFeil = 3
)
class KonsistensavstemMotOppdragAvsluttTask(
    val avstemmingService: AvstemmingService,
    val dataChunkRepository: DataChunkRepository,
    val batchService: BatchService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val konsistensavstemmingAvsluttTask =
            objectMapper.readValue(task.payload, KonsistensavstemmingAvsluttTaskDTO::class.java)

        val dataChunks = dataChunkRepository.findByTransaksjonsId(konsistensavstemmingAvsluttTask.transaksjonsId)
        if (dataChunks.any { !it.erSendt }) {
            throw RekjørSenereException(
                årsak = "Alle datatasks for konsistensavstemming med id ${konsistensavstemmingAvsluttTask.transaksjonsId} er ikke kjørt.",
                triggerTid = LocalDateTime.now().plusMinutes(15)
            )
        }

        if (konsistensavstemmingAvsluttTask.sendTilØkonomi) {
            avstemmingService.konsistensavstemOppdragAvslutt(
                avstemmingsdato = konsistensavstemmingAvsluttTask.avstemmingsdato,
                transaksjonsId = konsistensavstemmingAvsluttTask.transaksjonsId
            )
        } else {
            logger.info("Send til økonomi skrudd av for ${konsistensavstemmingAvsluttTask.transaksjonsId} for task $TASK_STEP_TYPE")
        }

        batchService.lagreNyStatus(konsistensavstemmingAvsluttTask.batchId, KjøreStatus.FERDIG)
    }

    companion object {
        const val TASK_STEP_TYPE = "konsistensavstemMotOppdragAvslutt"
        private val logger: Logger =
            LoggerFactory.getLogger(KonsistensavstemMotOppdragAvsluttTask::class.java)
    }
}
