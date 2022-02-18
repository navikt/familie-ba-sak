package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdragAvsluttTask
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdragDataTask
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingAvsluttTaskDTO
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingDataTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.PerioderForBehandling
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class AvstemmingService(
    val økonomiKlient: ØkonomiKlient,
    val behandlingService: BehandlingService,
    val beregningService: BeregningService,
    val taskRepository: TaskRepository,
    val batchRepository: BatchRepository,
    val dataChunkRepository: DataChunkRepository,
) {
    fun grensesnittavstemOppdrag(fraDato: LocalDateTime, tilDato: LocalDateTime) {

        Result.runCatching { økonomiKlient.grensesnittavstemOppdrag(fraDato, tilDato) }
            .fold(
                onSuccess = {
                    logger.debug("Grensesnittavstemming mot oppdrag utført.")
                },
                onFailure = {
                    logger.error("Grensesnittavstemming mot oppdrag feilet", it)
                    throw it
                }
            )
    }

    @Deprecated("Fjern når konsistensavstemming i batch er testet og virker")
    fun konsistensavstemOppdrag(avstemmingsdato: LocalDateTime) {

        val relevanteBehandlinger = behandlingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker()
        val perioderTilAvstemming = hentDataForKonsistensavstemming(avstemmingsdato, relevanteBehandlinger)

        logger.info("Utfører konsisensavstemming for ${perioderTilAvstemming.size} løpende saker")

        Result.runCatching { økonomiKlient.konsistensavstemOppdrag(avstemmingsdato, perioderTilAvstemming) }
            .fold(
                onSuccess = {
                    logger.debug("Konsistensavstemming mot oppdrag utført.")
                },
                onFailure = {
                    logger.error("Konsistensavstemming mot oppdrag feilet", it)
                    throw it
                }
            )
    }

    @Transactional
    fun konsistensavstemOppdragStart(batchId: Long, avstemmingsdato: LocalDateTime, transaksjonsId: UUID) {
        opprettKonsistensavstemmingDataTasker(avstemmingsdato, batchId, transaksjonsId)
        opprettKonsistensavstemmingAvsluttTask(batchId, transaksjonsId, avstemmingsdato)

        runCatching {
            økonomiKlient.konsistensavstemOppdragStart(
                avstemmingsdato,
                transaksjonsId
            )
        }
            .fold(
                onSuccess = {
                    logger.debug("Konsistensavstemming mot oppdrag med transaksjonsId $transaksjonsId startet.")
                },
                onFailure = {
                    logger.error(
                        "Starting av konsistensavstemming med transaksjonsId $transaksjonsId mot oppdrag feilet",
                        it
                    )
                    throw it
                }
            )
    }

    fun konsistensavstemOppdragData(
        avstemmingsdato: LocalDateTime,
        perioderTilAvstemming: List<PerioderForBehandling>,
        transaksjonsId: UUID,
        chunkNr: Int,
    ) {
        logger.info("Utfører konsisensavstemming: Sender perioder for transaksjonsId $transaksjonsId og chunk nr $chunkNr")
        val dataChunk = dataChunkRepository.findByTransaksjonsIdAndChunkNr(transaksjonsId, chunkNr)
        Result.runCatching {
            økonomiKlient.konsistensavstemOppdragData(
                avstemmingsdato,
                perioderTilAvstemming,
                transaksjonsId
            )
        }
            .fold(
                onSuccess = {
                    dataChunkRepository.save(dataChunk.also { it.erSendt = true })
                    logger.debug("Perioder til konsistensavstemming for transaksjonsId $transaksjonsId og chunk nr $chunkNr mot oppdrag utført.")
                },
                onFailure = {
                    logger.error("Perioder til konsistensavstemming mot oppdrag feilet", it)
                    throw it
                }
            )
    }

    fun konsistensavstemOppdragAvslutt(avstemmingsdato: LocalDateTime, transaksjonsId: UUID) {
        logger.info("Avslutter konsistensavstemming for $transaksjonsId")

        Result.runCatching { økonomiKlient.konsistensavstemOppdragAvslutt(avstemmingsdato, transaksjonsId) }
            .fold(
                onSuccess = {
                    logger.debug("Avslutt av Konsistensavstemming mot oppdrag for $transaksjonsId utført.")
                },
                onFailure = {
                    logger.error("Avslutt av Konsistensavstemming for $transaksjonsId mot oppdrag feilet", it)
                    throw it
                }
            )
    }

    private fun opprettKonsistensavstemmingAvsluttTask(
        batchId: Long,
        transaksjonsId: UUID,
        avstemmingsdato: LocalDateTime
    ) {
        logger.info("Oppretter avsluttingstask for transaksjonsId $transaksjonsId")
        val konsistensavstemmingAvsluttTask = Task(
            type = KonsistensavstemMotOppdragAvsluttTask.TASK_STEP_TYPE,
            payload = objectMapper.writeValueAsString(
                KonsistensavstemmingAvsluttTaskDTO(
                    batchId = batchId,
                    transaksjonsId = transaksjonsId,
                    avstemmingsdato = avstemmingsdato,
                )
            )
        )
        taskRepository.save(konsistensavstemmingAvsluttTask)
    }

    private fun opprettKonsistensavstemmingDataTasker(
        avstemmingsdato: LocalDateTime,
        batchId: Long,
        transaksjonsId: UUID
    ) {
        var relevanteBehandlinger =
            behandlingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(
                Pageable.ofSize(
                    KONSISTENSAVSTEMMING_DATA_CHUNK_STORLEK
                )
            )

        for (chunkNr in 1..relevanteBehandlinger.totalPages) {
            val perioderTilAvstemming =
                hentDataForKonsistensavstemming(
                    avstemmingsdato,
                    relevanteBehandlinger.content.map { it.toLong() }
                )
            val batch = batchRepository.getById(batchId)

            logger.info("Oppretter konsisensavstemmingstasker for transaksjonsId $transaksjonsId og chunk $chunkNr med ${perioderTilAvstemming.size} løpende saker")
            val konsistensavstemmingDataTask = Task(
                type = KonsistensavstemMotOppdragDataTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(
                    KonsistensavstemmingDataTaskDTO(
                        transaksjonsId = transaksjonsId,
                        chunkNr = chunkNr,
                        avstemmingdato = avstemmingsdato,
                        perioderForBehandling = perioderTilAvstemming,
                    )
                )
            )
            taskRepository.save(konsistensavstemmingDataTask)
            dataChunkRepository.save(DataChunk(batch = batch, transaksjonsId = transaksjonsId, chunkNr = chunkNr))

            relevanteBehandlinger =
                behandlingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(relevanteBehandlinger.nextPageable())
        }
    }

    private fun hentDataForKonsistensavstemming(
        avstemmingstidspunkt: LocalDateTime,
        relevanteBehandlinger: List<Long>
    ): List<PerioderForBehandling> {
        return relevanteBehandlinger
            .chunked(1000)
            .map { chunk ->
                val relevanteAndeler = beregningService.hentLøpendeAndelerTilkjentYtelseMedUtbetalingerForBehandlinger(
                    behandlingIder = chunk,
                    avstemmingstidspunkt = avstemmingstidspunkt
                )
                relevanteAndeler.groupBy { it.kildeBehandlingId }
                    .map { (kildeBehandlingId, andeler) ->
                        PerioderForBehandling(
                            behandlingId = kildeBehandlingId.toString(),
                            perioder = andeler
                                .map {
                                    it.periodeOffset
                                        ?: error("Andel ${it.id} på iverksatt behandling på løpende fagsak mangler periodeOffset")
                                }
                                .toSet()
                        )
                    }
            }.flatten()
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(AvstemmingService::class.java)

        const val KONSISTENSAVSTEMMING_DATA_CHUNK_STORLEK = 50
    }
}
