package no.nav.familie.ba.sak.integrasjoner.økonomi

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import junit.framework.Assert.assertEquals
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdragAvsluttTask
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdragDataTask
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class AvstemmingServiceTest {
    val økonomiKlient = mockk<ØkonomiKlient>()
    val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    val beregningService = mockk<BeregningService>()
    val taskRepository = mockk<TaskRepository>(relaxed = true)
    val batchRepository = mockk<BatchRepository>()
    val dataChunkRepository = mockk<DataChunkRepository>(relaxed = true)

    val avstemmingService = AvstemmingService(
        behandlingHentOgPersisterService,
        økonomiKlient,
        beregningService,
        taskRepository,
        batchRepository,
        dataChunkRepository
    )

    val batchId = 1000000L
    val avstemmingsdato = LocalDateTime.now()
    val transaksjonsId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        val page = mockk<Page<BigInteger>>()
        val pageable = Pageable.ofSize(AvstemmingService.KONSISTENSAVSTEMMING_DATA_CHUNK_STORLEK)
        every { behandlingHentOgPersisterService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(pageable) } returns page
        every { page.totalPages } returns 1
        every { page.content } returns listOf(BigInteger.ONE)
        every { page.nextPageable() } returns pageable

        every {
            beregningService.hentLøpendeAndelerTilkjentYtelseMedUtbetalingerForBehandlinger(
                any(),
                any()
            )
        } returns listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.now().minusMonths(4),
                tom = YearMonth.now(),
                periodeIdOffset = 0
            )
        )
        every { batchRepository.getById(batchId) } returns Batch(id = batchId, kjøreDato = LocalDate.now())
        every { taskRepository.save(any()) } returns Task(type = "dummy", payload = "")
        every { dataChunkRepository.save(any()) } returns DataChunk(
            batch = Batch(kjøreDato = LocalDate.now()),
            chunkNr = 1,
            transaksjonsId = transaksjonsId
        )
    }

    @Test
    fun `Verifiser at konsistensavstemOppdragStart oppretter data- og avslutt task og sender start melding`() {
        every {
            økonomiKlient.konsistensavstemOppdragStart(
                avstemmingsdato,
                transaksjonsId
            )
        } returns ""

        konsistensavstemOppdragStart(
            batchId = batchId,
            avstemmingsdato = avstemmingsdato,
            transaksjonsId = transaksjonsId
        )

        val taskSlots = mutableListOf<Task>()
        verify(exactly = 2) { taskRepository.save(capture(taskSlots)) }

        val dataChunkSlot = slot<DataChunk>()
        verify(exactly = 1) { dataChunkRepository.save(capture(dataChunkSlot)) }

        verify(exactly = 1) {
            økonomiKlient.konsistensavstemOppdragStart(
                avstemmingsdato = avstemmingsdato,
                transaksjonsId = transaksjonsId
            )
        }

        assertEquals(1, dataChunkSlot.captured.chunkNr)
        assertEquals(transaksjonsId, dataChunkSlot.captured.transaksjonsId)

        assertEquals(KonsistensavstemMotOppdragDataTask.TASK_STEP_TYPE, taskSlots[0].type)
        assertEquals(KonsistensavstemMotOppdragAvsluttTask.TASK_STEP_TYPE, taskSlots[1].type)
    }

    @Test
    fun `Verifiser at konsistensavstemOppdragData sender data og oppdatere datachunk tabellen`() {
        every { dataChunkRepository.findByTransaksjonsIdAndChunkNr(transaksjonsId, 1) } returns
            DataChunk(
                batch = Batch(id = batchId, kjøreDato = LocalDate.now()),
                transaksjonsId = transaksjonsId,
                chunkNr = 1
            )
        every {
            økonomiKlient.konsistensavstemOppdragData(
                avstemmingsdato,
                emptyList(),
                transaksjonsId,
            )
        } returns ""

        avstemmingService.konsistensavstemOppdragData(
            avstemmingsdato = avstemmingsdato,
            transaksjonsId = transaksjonsId,
            chunkNr = 1,
            perioderTilAvstemming = emptyList(),
        )

        val dataChunkSlot = slot<DataChunk>()
        verify(exactly = 1) { dataChunkRepository.save(capture(dataChunkSlot)) }

        verify(exactly = 1) {
            økonomiKlient.konsistensavstemOppdragData(
                avstemmingsdato = avstemmingsdato,
                perioderTilAvstemming = emptyList(),
                transaksjonsId = transaksjonsId
            )
        }

        assertEquals(1, dataChunkSlot.captured.chunkNr)
        assertEquals(transaksjonsId, dataChunkSlot.captured.transaksjonsId)
        assertEquals(true, dataChunkSlot.captured.erSendt)
    }

    private fun konsistensavstemOppdragStart(batchId: Long, avstemmingsdato: LocalDateTime, transaksjonsId: UUID) {
        avstemmingService.nullstillDataChunk()

        avstemmingService.sendKonsistensavstemmingStart(avstemmingsdato, transaksjonsId)

        var relevanteBehandlinger =
            avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker()

        for (chunkNr in 1..relevanteBehandlinger.totalPages) {
            avstemmingService.opprettKonsistensavstemmingDataTask(
                avstemmingsdato,
                relevanteBehandlinger,
                batchId,
                transaksjonsId,
                chunkNr
            )
            relevanteBehandlinger =
                avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(relevanteBehandlinger.nextPageable())
        }

        avstemmingService.opprettKonsistensavstemmingAvsluttTask(batchId, transaksjonsId, avstemmingsdato)
    }
}
