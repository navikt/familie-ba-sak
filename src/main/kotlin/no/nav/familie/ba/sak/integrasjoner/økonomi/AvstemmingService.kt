package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdragAvslutt
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdragData
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingAvsluttTaskDTO
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingDataTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.PerioderForBehandling
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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

        val perioderTilAvstemming = hentDataForKonsistensavstemming(avstemmingsdato)

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
        val batchStorlek = 1000
        // TODO: legg in paging eller streaming.
        val perioderTilAvstemming = hentDataForKonsistensavstemming(avstemmingsdato)
        val batch = batchRepository.getById(batchId)

        logger.info("Oppretter konsisensavstemmingstasker for ${perioderTilAvstemming.size} løpende saker")

        var chunkNr = 0
        perioderTilAvstemming.chunked(batchStorlek) {
            val konsistensavstemmingDataTask = Task(
                type = KonsistensavstemMotOppdragData.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(
                    KonsistensavstemmingDataTaskDTO(
                        transaksjonsId = transaksjonsId,
                        chunkNr = ++chunkNr,
                        avstemmingdato = avstemmingsdato,
                        perioderForBehandling = it,
                    )
                )
            )
            taskRepository.save(konsistensavstemmingDataTask)
            dataChunkRepository.save(DataChunk(batch = batch, transaksjonsId = transaksjonsId, chunkNr = chunkNr))
        }

        val konsistensavstemmingAvsluttTask = Task(
            type = KonsistensavstemMotOppdragAvslutt.TASK_STEP_TYPE,
            payload = objectMapper.writeValueAsString(
                KonsistensavstemmingAvsluttTaskDTO(
                    batchId = batchId,
                    transaksjonsId = transaksjonsId,
                    avstemmingsdato = avstemmingsdato,
                )
            )
        )
        taskRepository.save(konsistensavstemmingAvsluttTask)

        runCatching {
            økonomiKlient.konsistensavstemOppdragStart(
                avstemmingsdato,
                transaksjonsId
            )
        }
            .fold(
                onSuccess = {
                    logger.debug("Konsistensavstemming mot oppdrag startet.")
                },
                onFailure = {
                    logger.error("Starting av konsistensavstemming mot oppdrag feilet", it)
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
        logger.info("Utfører konsisensavstemming: Sender perioder for transaksjonsId $transaksjonsId")
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
                    logger.debug("Perioder til konsistensavstemming mot oppdrag utført.")
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
                    logger.debug("Avslutt av Konsistensavstemming mot oppdrag utført.")
                },
                onFailure = {
                    logger.error("Avslutt av Konsistensavstemming mot oppdrag feilet", it)
                    throw it
                }
            )
    }

    private fun hentDataForKonsistensavstemming(avstemmingstidspunkt: LocalDateTime): List<PerioderForBehandling> {
        val relevanteBehandlinger = behandlingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker()
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
    }
}
