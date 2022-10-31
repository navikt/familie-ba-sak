package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdragAvsluttTask
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdragPerioderGeneratorTask
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingAvsluttTaskDTO
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingPerioderGeneratorTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.PerioderForBehandling
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class AvstemmingService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val økonomiKlient: ØkonomiKlient,
    private val beregningService: BeregningService,
    private val taskRepository: TaskRepository,
    private val batchRepository: BatchRepository,
    private val dataChunkRepository: DataChunkRepository
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

    fun erKonsistensavstemmingKjørtForTransaksjonsid(transaksjonsId: UUID): Boolean {
        val dataChunks = dataChunkRepository.findByTransaksjonsId(transaksjonsId)
        return dataChunks.none { !it.erSendt } && dataChunks.isNotEmpty()
    }

    fun erKonsistensavstemmingDelvisKjørtForTransaksjonsid(transaksjonsId: UUID): Boolean {
        val dataChunks = dataChunkRepository.findByTransaksjonsId(transaksjonsId)
        return dataChunks.any { !it.erSendt } && dataChunks.isNotEmpty()
    }

    fun skalOppretteKonsistensavstemingPeriodeGeneratorTask(transaksjonsId: UUID, chunkNr: Int): Boolean {
        return dataChunkRepository.findByTransaksjonsIdAndChunkNr(transaksjonsId, chunkNr) == null
    }

    fun erKonsistensavstemmingKjørtForTransaksjonsidOgChunk(transaksjonsId: UUID, chunkNr: Int): Boolean {
        val dataChunk = dataChunkRepository.findByTransaksjonsIdAndChunkNr(transaksjonsId, chunkNr)
        return dataChunk?.erSendt == true
    }

    fun konsistensavstemOppdragData(
        avstemmingsdato: LocalDateTime,
        perioderTilAvstemming: List<PerioderForBehandling>,
        transaksjonsId: UUID,
        chunkNr: Int,
        sendTilØkonomi: Boolean
    ) {
        logger.info("Utfører konsisensavstemming: Sender perioder for transaksjonsId $transaksjonsId og chunk nr $chunkNr")
        val dataChunk = dataChunkRepository.findByTransaksjonsIdAndChunkNr(transaksjonsId, chunkNr)
            ?: error("Finner ingen datachunk for $transaksjonsId og $chunkNr")

        if (dataChunk.erSendt) {
            logger.info("Utfører konsisensavstemming: Perioder for transaksjonsId $transaksjonsId og chunk nr $chunkNr er allerede sendt.")
            return
        }

        if (sendTilØkonomi) {
            økonomiKlient.konsistensavstemOppdragData(
                avstemmingsdato,
                perioderTilAvstemming,
                transaksjonsId
            )
        } else {
            logger.info("Send til økonomi skrudd av for $transaksjonsId for task konsistensavstemOppdragData")
        }

        dataChunkRepository.save(dataChunk.also { it.erSendt = true })
    }

    fun konsistensavstemOppdragAvslutt(avstemmingsdato: LocalDateTime, transaksjonsId: UUID) {
        logger.info("Avslutter konsistensavstemming for $transaksjonsId")

        økonomiKlient.konsistensavstemOppdragAvslutt(avstemmingsdato, transaksjonsId)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprettKonsistensavstemmingAvsluttTask(
        konsistensavstemmingAvsluttTaskDTO: KonsistensavstemmingAvsluttTaskDTO
    ) {
        logger.info("Oppretter avsluttingstask for transaksjonsId ${konsistensavstemmingAvsluttTaskDTO.transaksjonsId}")
        val konsistensavstemmingAvsluttTask = Task(
            type = KonsistensavstemMotOppdragAvsluttTask.TASK_STEP_TYPE,
            payload = objectMapper.writeValueAsString(konsistensavstemmingAvsluttTaskDTO)
        )
        taskRepository.save(konsistensavstemmingAvsluttTask)
    }

    fun hentSisteIverksatteBehandlingerFraLøpendeFagsaker(
        pageable: Pageable = Pageable.ofSize(KONSISTENSAVSTEMMING_DATA_CHUNK_STORLEK)
    ) =
        behandlingHentOgPersisterService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(pageable)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprettKonsistensavstemmingPerioderGeneratorTask(
        konsistensavstemmingPerioderGeneratorTaskDTO: KonsistensavstemmingPerioderGeneratorTaskDTO
    ) {
        val batch = batchRepository.getReferenceById(konsistensavstemmingPerioderGeneratorTaskDTO.batchId)
        dataChunkRepository.save(
            DataChunk(
                batch = batch,
                transaksjonsId = konsistensavstemmingPerioderGeneratorTaskDTO.transaksjonsId,
                chunkNr = konsistensavstemmingPerioderGeneratorTaskDTO.chunkNr
            )
        )

        logger.info("Oppretter task for å generere perioder for relevante behandlinger. transaksjonsId=${konsistensavstemmingPerioderGeneratorTaskDTO.transaksjonsId} og chunk=${konsistensavstemmingPerioderGeneratorTaskDTO.chunkNr} med ${konsistensavstemmingPerioderGeneratorTaskDTO.relevanteBehandlinger.size} behandlinger")
        val task = Task(
            type = KonsistensavstemMotOppdragPerioderGeneratorTask.TASK_STEP_TYPE,
            payload = objectMapper.writeValueAsString(
                konsistensavstemmingPerioderGeneratorTaskDTO
            )
        )
        taskRepository.save(task)
    }

    fun hentDataForKonsistensavstemming(
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
                val aktiveFødselsnummere =
                    behandlingHentOgPersisterService.hentAktivtFødselsnummerForBehandlinger(
                        relevanteAndeler.mapNotNull { it.kildeBehandlingId }
                    )

                relevanteAndeler.groupBy { it.kildeBehandlingId }
                    .map { (kildeBehandlingId, andeler) ->
                        if (kildeBehandlingId == null) {
                            secureLogger.warn("Finner ikke behandlingsId for andeler=$andeler")
                        }
                        PerioderForBehandling(
                            behandlingId = kildeBehandlingId.toString(),
                            aktivFødselsnummer = aktiveFødselsnummere[kildeBehandlingId]
                                ?: error("Finnes ikke et aktivt fødselsnummer for behandling $kildeBehandlingId"),
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
