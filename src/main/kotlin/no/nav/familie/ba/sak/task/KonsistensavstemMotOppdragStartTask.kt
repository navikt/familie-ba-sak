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
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = KonsistensavstemMotOppdragStartTask
        .TASK_STEP_TYPE,
    beskrivelse = "Start Konsistensavstemming mot oppdrag",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true
)
class KonsistensavstemMotOppdragStartTask(val avstemmingService: AvstemmingService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val konsistensavstemmingTask =
            objectMapper.readValue(task.payload, KonsistensavstemmingStartTaskDTO::class.java)
        val transaksjonsId = UUID.randomUUID()

        val avstemmingsDato = LocalDateTime.now()
        logger.info("Konsistensavstemming ble initielt trigget ${konsistensavstemmingTask.avstemmingdato}, men bruker $avstemmingsDato som avstemmingsdato")

        avstemmingService.nullstillDataChunk()
        avstemmingService.sendKonsistensavstemmingStart(avstemmingsDato, transaksjonsId)

        var relevanteBehandlinger =
            avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(Pageable.ofSize(ANTALL_BEHANDLINGER))

        var chunkNr = 1
        for (pageNumber in 1..relevanteBehandlinger.totalPages) {
            relevanteBehandlinger.content.chunked(AvstemmingService.KONSISTENSAVSTEMMING_DATA_CHUNK_STORLEK)
                .forEach { oppstykketRelevanteBehandlinger ->
                    avstemmingService.opprettKonsistensavstemmingDataTask(
                        avstemmingsDato,
                        oppstykketRelevanteBehandlinger,
                        konsistensavstemmingTask.batchId,
                        transaksjonsId,
                        chunkNr
                    )
                    chunkNr = chunkNr.inc()
                }
            relevanteBehandlinger =
                avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(relevanteBehandlinger.nextPageable())
        }

        avstemmingService.opprettKonsistensavstemmingAvsluttTask(
            konsistensavstemmingTask.batchId,
            transaksjonsId,
            avstemmingsDato
        )
    }

    fun dryRunKonsistensavstemmingOmskriving(size: Int) {
        logger.info("[dryRun-konsinstenavstemming] Start: hent behandlinger klar for konsistensavstemming omskriving ${LocalDateTime.now()}")
        val transaksjonsId = UUID.randomUUID()
        val sideantall = Pageable.ofSize(size)
        var relevanteBehandlinger = avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(sideantall)
        logger.info("[dryRun-konsinstenavstemming] Antall sider: ${relevanteBehandlinger.totalPages} og antallBehandlinger ${relevanteBehandlinger.size}")
        val loggBehandlinger = mutableListOf<BigInteger>()
        var chunkNr = 0

        for (pageNumber in 1..2) {
            relevanteBehandlinger.chunked(500).forEach { behandlinger ->
                loggBehandlinger.addAll(behandlinger)
                avstemmingService.opprettKonsistensavstemmingDataTaskDryrun(
                    LocalDateTime.now(),
                    behandlinger,
                    transaksjonsId,
                    chunkNr
                )
                chunkNr = chunkNr.inc()
            }
            relevanteBehandlinger =
                avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(relevanteBehandlinger.nextPageable())
        }

        logger.info("[dryRun-konsinstenavstemming] Slutt: hent behandlinger klar for konsistensavstemming omskriving${LocalDateTime.now()}")
        logger.info("[dryRun-konsinstenavstemming] behandlinger: $loggBehandlinger")
    }

    companion object {
        const val TASK_STEP_TYPE = "konsistensavstemMotOppdragStart"
        const val ANTALL_BEHANDLINGER = 10000
        private val logger = LoggerFactory.getLogger(KonsistensavstemMotOppdragStartTask::class.java)
    }
}
