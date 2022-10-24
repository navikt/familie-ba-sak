package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.integrasjoner.økonomi.AvstemmingService
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingStartTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = KonsistensavstemMotOppdragStartTask
        .TASK_STEP_TYPE,
    beskrivelse = "Start Konsistensavstemming mot oppdrag",
    maxAntallFeil = 3
)
class KonsistensavstemMotOppdragStartTask(val avstemmingService: AvstemmingService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val konsistensavstemmingTask =
            objectMapper.readValue(task.payload, KonsistensavstemmingStartTaskDTO::class.java)
        val transaksjonsId = UUID.randomUUID()

        avstemmingService.nullstillDataChunk()
        avstemmingService.sendKonsistensavstemmingStart(konsistensavstemmingTask.avstemmingdato, transaksjonsId)

        var relevanteBehandlinger = avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker()

        for (chunkNr in 1..relevanteBehandlinger.totalPages) {
            avstemmingService.opprettKonsistensavstemmingDataTask(
                konsistensavstemmingTask.avstemmingdato,
                relevanteBehandlinger,
                konsistensavstemmingTask.batchId,
                transaksjonsId,
                chunkNr
            )
            relevanteBehandlinger =
                avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(relevanteBehandlinger.nextPageable())
        }

        avstemmingService.opprettKonsistensavstemmingAvsluttTask(
            konsistensavstemmingTask.batchId,
            transaksjonsId,
            konsistensavstemmingTask.avstemmingdato
        )
    }

    fun dryRunKonsistensavstemming() {
        logger.info("Start: hent behandlinger klar for konsistensavstemming ${LocalDateTime.now()}")
        var relevanteBehandlinger = avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker()

        for (chunkNr in 1..100) {
            logger.info("Chunk $chunkNr: hent behandlinger klar for konsistensavstemming ${LocalDateTime.now()}")
            relevanteBehandlinger =
                avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(relevanteBehandlinger.nextPageable())
        }
        logger.info("Slutt: hent behandlinger klar for konsistensavstemming ${LocalDateTime.now()}")
    }

    fun dryRunKonsistensavstemmingOmskriving(size: Int) {
        logger.info("Start: hent behandlinger klar for konsistensavstemming omskriving ${LocalDateTime.now()}")
        val sideantall = Pageable.ofSize(size)
        var relevanteBehandlinger = avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(sideantall)
        logger.info("Antall sider: ${relevanteBehandlinger.totalPages} og antallBehandlinger ${relevanteBehandlinger.size}")
        var chunkNr = 0

        for (pageNumber in 1..10) {
            relevanteBehandlinger.chunked(500).forEach { behandlinger ->
                logger.info("Chunk $chunkNr: hent behandlinger klar for konsistensavstemming omskriving ${LocalDateTime.now()}")
                chunkNr = chunkNr.inc()
            }
            relevanteBehandlinger =
                avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(relevanteBehandlinger.nextPageable())
            logger.info("Antall sider: $relevanteBehandlinger og antallBehandlinger ${relevanteBehandlinger.size}")
        }

        logger.info("Slutt: hent behandlinger klar for konsistensavstemming omskriving${LocalDateTime.now()}")
    }

    companion object {
        const val TASK_STEP_TYPE = "konsistensavstemMotOppdragStart"
        private val logger = LoggerFactory.getLogger(KonsistensavstemMotOppdragStartTask::class.java)
    }
}
