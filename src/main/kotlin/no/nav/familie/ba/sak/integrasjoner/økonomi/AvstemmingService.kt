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
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
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

        økonomiKlient.grensesnittavstemOppdrag(fraDato, tilDato)
    }

    fun sendKonsistensavstemmingStart(avstemmingsdato: LocalDateTime, transaksjonsId: UUID) {
        økonomiKlient.konsistensavstemOppdragStart(
            avstemmingsdato,
            transaksjonsId
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun nullstillDataChunk() {
        dataChunkRepository.saveAllAndFlush(
            dataChunkRepository.findByErSendt(false).map { dataChunk -> dataChunk.also { it.erSendt = true } }
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

        if (dataChunk.erSendt) {
            logger.info("Utfører konsisensavstemming: Perioder for transaksjonsId $transaksjonsId og chunk nr $chunkNr er allerede sendt.")
            return
        }

        økonomiKlient.konsistensavstemOppdragData(
            avstemmingsdato,
            perioderTilAvstemming,
            transaksjonsId
        )

        dataChunkRepository.save(dataChunk.also { it.erSendt = true })
    }

    fun konsistensavstemOppdragAvslutt(avstemmingsdato: LocalDateTime, transaksjonsId: UUID) {
        logger.info("Avslutter konsistensavstemming for $transaksjonsId")

        økonomiKlient.konsistensavstemOppdragAvslutt(avstemmingsdato, transaksjonsId)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprettKonsistensavstemmingAvsluttTask(
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

    fun hentSisteIverksatteBehandlingerFraLøpendeFagsaker(
        pageable: Pageable = Pageable.ofSize(KONSISTENSAVSTEMMING_DATA_CHUNK_STORLEK)
    ) =
        behandlingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(pageable)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprettKonsistensavstemmingDataTask(
        avstemmingsdato: LocalDateTime,
        relevanteBehandlinger: Page<BigInteger>,
        batchId: Long,
        transaksjonsId: UUID,
        chunkNr: Int
    ) {
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

        const val KONSISTENSAVSTEMMING_DATA_CHUNK_STORLEK = 500
    }
}
