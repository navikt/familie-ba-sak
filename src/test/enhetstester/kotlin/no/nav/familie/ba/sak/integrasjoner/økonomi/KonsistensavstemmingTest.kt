package no.nav.familie.ba.sak.integrasjoner.økonomi

import io.mockk.CapturingSlot
import io.mockk.clearAllMocks
import io.mockk.clearStaticMockk
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdragAvsluttTask
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdragDataTask
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdragStartTask
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingDataTaskDTO
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingStartTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KonsistensavstemmingTest {
    private val økonomiKlient = mockk<ØkonomiKlient>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val beregningService = mockk<BeregningService>()
    private val taskRepository = mockk<TaskRepository>(relaxed = true)
    private val batchRepository = mockk<BatchRepository>()
    private val dataChunkRepository = mockk<DataChunkRepository>(relaxed = true)

    private val avstemmingService = AvstemmingService(
        behandlingHentOgPersisterService,
        økonomiKlient,
        beregningService,
        taskRepository,
        batchRepository,
        dataChunkRepository
    )

    private val batchId = 1000000L
    private val behandlingId = BigInteger.ONE
    private val avstemmingsdato = LocalDateTime.now()
    private val transaksjonsId = UUID.randomUUID()
    private val konsistensavstemmingStartTaskDTO =
        KonsistensavstemmingStartTaskDTO(batchId, avstemmingsdato, transaksjonsId)
    private val perioderForRelevanteBehandlingerDT =
        KonsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO(
            batchId,
            transaksjonsId,
            avstemmingsdato,
            1,
            listOf(behandlingId.toLong())
        )

    private lateinit var konistensavstemmingStartTask: KonsistensavstemMotOppdragStartTask
    private lateinit var konsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask: KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask
    private lateinit var konsistensavstemMotOppdragDataTask: KonsistensavstemMotOppdragDataTask
    private lateinit var konsistensavstemMotOppdragAvsluttTask: KonsistensavstemMotOppdragAvsluttTask

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        val uuidMock = mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns transaksjonsId
        every { taskRepository.save(any()) } returns Task(type = "dummy", payload = "")
        konistensavstemmingStartTask = KonsistensavstemMotOppdragStartTask(avstemmingService)
        konsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask =
            KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask(avstemmingService, taskRepository)
        konsistensavstemMotOppdragDataTask = KonsistensavstemMotOppdragDataTask(avstemmingService)
        konsistensavstemMotOppdragAvsluttTask =
            KonsistensavstemMotOppdragAvsluttTask(avstemmingService, dataChunkRepository, BatchService(batchRepository))
    }

    @AfterAll
    fun teardown() {
        clearStaticMockk(UUID::class)
    }

    @Test
    fun `Første gangs kjøring av start task - Verifiser at konsistensavstemOppdragStart oppretter finn perioder for relevante behandlinger task- og avslutt task og sender start melding hvis transaksjon ikke allerede kjørt`() {
        val avstemmingsdatoSlot = lagMockForStartTaskHappCase()

        konistensavstemmingStartTask.doTask(
            Task(
                payload = objectMapper.writeValueAsString(konsistensavstemmingStartTaskDTO),
                type = KonsistensavstemMotOppdragStartTask.TASK_STEP_TYPE
            )
        )

        val taskSlots = mutableListOf<Task>()
        verify(exactly = 2) { taskRepository.save(capture(taskSlots)) }

        val dataChunkSlot = slot<DataChunk>()
        verify(exactly = 1) { dataChunkRepository.save(capture(dataChunkSlot)) }

        verify(exactly = 1) {
            økonomiKlient.konsistensavstemOppdragStart(
                avstemmingsdato = avstemmingsdatoSlot.captured,
                transaksjonsId = transaksjonsId
            )
        }

        assertEquals(1, dataChunkSlot.captured.chunkNr)
        assertEquals(transaksjonsId, dataChunkSlot.captured.transaksjonsId)

        assertEquals(
            KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.TASK_STEP_TYPE,
            taskSlots[0].type
        )
        val finnPerioderForRelevanteBehandlingerDto =
            objectMapper.readValue(
                taskSlots[0].payload,
                KonsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO::class.java
            )
        assertEquals(batchId, finnPerioderForRelevanteBehandlingerDto.batchId)
        assertEquals(transaksjonsId, finnPerioderForRelevanteBehandlingerDto.transaksjonsId)
        assertEquals(1, finnPerioderForRelevanteBehandlingerDto.chunkNr)
        assertThat(finnPerioderForRelevanteBehandlingerDto.relevanteBehandlinger).hasSize(1).containsExactly(1)
        assertEquals(KonsistensavstemMotOppdragAvsluttTask.TASK_STEP_TYPE, taskSlots[1].type)
        assertThat(avstemmingsdatoSlot.captured)
            .isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
    }

    @Test
    fun `Rekjøring av start task - Verifiser at konsistensavstemming ikke kjører hvis alle datachunker allerede er sendt til økonomi for transaksjonId`() {
        every { batchRepository.getReferenceById(batchId) } returns Batch(
            kjøreDato = LocalDate.now(),
            status = KjøreStatus.FERDIG
        )

        konistensavstemmingStartTask.doTask(
            Task(
                payload = objectMapper.writeValueAsString(konsistensavstemmingStartTaskDTO),
                type = KonsistensavstemMotOppdragStartTask.TASK_STEP_TYPE
            )
        )

        verify(exactly = 0) { avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(any()) }
    }

    @Test
    fun `Rekjøring av start task - Verifiser at konsistensavstemming kun rekjører chunker som ikke allerede er kjørt`() {
        lagMockForStartTaskHappCase()
        val datachunks = listOf(
            DataChunk(
                batch = Batch(kjøreDato = LocalDate.now()),
                transaksjonsId = transaksjonsId,
                erSendt = true,
                chunkNr = 1
            ),
            DataChunk(
                batch = Batch(kjøreDato = LocalDate.now()),
                transaksjonsId = transaksjonsId,
                erSendt = false,
                chunkNr = 2
            )
        )
        every { dataChunkRepository.findByTransaksjonsIdAndChunkNr(transaksjonsId, 1) } returns datachunks[0]
        every { dataChunkRepository.findByTransaksjonsIdAndChunkNr(transaksjonsId, 2) } returns datachunks[1]
        every { dataChunkRepository.findByTransaksjonsIdAndChunkNr(transaksjonsId, 3) } returns null

        val page = mockk<Page<BigInteger>>()
        val pageable = Pageable.ofSize(KonsistensavstemMotOppdragStartTask.ANTALL_BEHANDLINGER)
        every { behandlingHentOgPersisterService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(pageable) } returns page
        every { page.totalPages } returns 1
        every { page.content } returns (1..1450).toList().map { it.toBigInteger() }
        every { page.nextPageable() } returns pageable

        konistensavstemmingStartTask.doTask(
            Task(
                payload = objectMapper.writeValueAsString(konsistensavstemmingStartTaskDTO),
                type = KonsistensavstemMotOppdragStartTask.TASK_STEP_TYPE
            )
        )

        verify(exactly = 2) { avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(any()) }
        val taskSlots = mutableListOf<Task>()
        verify(exactly = 2) { taskRepository.save(capture(taskSlots)) }

        assertEquals(
            KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.TASK_STEP_TYPE,
            taskSlots[0].type
        )
        val finnPerioderForRelevanteBehandlingerDto =
            objectMapper.readValue(
                taskSlots[0].payload,
                KonsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO::class.java
            )
        assertEquals(batchId, finnPerioderForRelevanteBehandlingerDto.batchId)
        assertEquals(transaksjonsId, finnPerioderForRelevanteBehandlingerDto.transaksjonsId)
        assertEquals(3, finnPerioderForRelevanteBehandlingerDto.chunkNr)
        assertThat(finnPerioderForRelevanteBehandlingerDto.relevanteBehandlinger).hasSize(450)

        assertEquals(KonsistensavstemMotOppdragAvsluttTask.TASK_STEP_TYPE, taskSlots[1].type)
    }

    @Test
    fun `Verifiser at konsistensavstemPeriodeFinnPerioderForRelevanteBehandlingerTask finner perioder for behandlinger og oppretter data task`() {
        lagMockFinnPerioderForRelevanteBehandlingerHappeCase()

        konsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.doTask(
            Task(
                payload = objectMapper.writeValueAsString(perioderForRelevanteBehandlingerDT),
                type = KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.TASK_STEP_TYPE
            )
        )

        val taskSlots = mutableListOf<Task>()
        verify(exactly = 1) { taskRepository.save(capture(taskSlots)) }
        val konsistensavstemmingDataDto =
            objectMapper.readValue(taskSlots[0].payload, KonsistensavstemmingDataTaskDTO::class.java)
        assertEquals(konsistensavstemmingDataDto.chunkNr, 1)
        assertEquals(konsistensavstemmingDataDto.transaksjonsId, transaksjonsId)
        assertThat(konsistensavstemmingDataDto.perioderForBehandling)
            .hasSize(1).extracting("behandlingId").containsExactly(behandlingId.toString())
    }

    @Test
    fun `Verifiser at konsistensavstemOppdragData sender data og oppdatere datachunk tabellen`() {
        lagMockOppdragDataHappeCase()
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
                transaksjonsId
            )
        } returns ""

        konsistensavstemMotOppdragDataTask.doTask(
            Task(
                payload = objectMapper.writeValueAsString(
                    KonsistensavstemmingDataTaskDTO(
                        transaksjonsId = transaksjonsId,
                        chunkNr = 1,
                        avstemmingdato = avstemmingsdato,
                        perioderForBehandling = emptyList(),
                        sendTilØkonomi = true
                    )
                ),
                type = KonsistensavstemMotOppdragDataTask.TASK_STEP_TYPE
            )

        )

        val dataChunkSlot = slot<DataChunk>()
        verify(exactly = 1) { dataChunkRepository.save(capture(dataChunkSlot)) }
        assertThat(dataChunkSlot.captured.erSendt).isTrue()

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

    @Test
    fun `Kjør alle tasker med input generert fra task som oppretter tasken`() {
        lagMockForStartTaskHappCase()
        konistensavstemmingStartTask.doTask(
            Task(
                payload = objectMapper.writeValueAsString(konsistensavstemmingStartTaskDTO),
                type = KonsistensavstemMotOppdragStartTask.TASK_STEP_TYPE
            )
        )
        val taskSlots = mutableListOf<Task>()
        verify(exactly = 2) { taskRepository.save(capture(taskSlots)) }

        lagMockFinnPerioderForRelevanteBehandlingerHappeCase()
        val finnPerioderForRelevanteBehandlingerTask =
            taskSlots.find { it.type == KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.TASK_STEP_TYPE }!!
        konsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.doTask(
            Task(
                payload = finnPerioderForRelevanteBehandlingerTask.payload,
                type = KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.TASK_STEP_TYPE
            )
        )
        verify(exactly = 3) { taskRepository.save(capture(taskSlots)) }

        lagMockOppdragDataHappeCase()
        konsistensavstemMotOppdragDataTask.doTask(
            Task(
                payload = taskSlots.first { it.type == KonsistensavstemMotOppdragDataTask.TASK_STEP_TYPE }.payload,
                type = KonsistensavstemMotOppdragDataTask.TASK_STEP_TYPE
            )
        )
        verify(exactly = 3) { taskRepository.save(capture(taskSlots)) }
        val datachunksSlot = mutableListOf<DataChunk>()
        verify(exactly = 2) { dataChunkRepository.save(capture(datachunksSlot)) }
        assertThat(datachunksSlot.last().erSendt).isTrue()

        val dataTask =
            taskSlots.find { it.type == KonsistensavstemMotOppdragDataTask.TASK_STEP_TYPE }!!
        val dataTaskDto = objectMapper.readValue(dataTask.payload, KonsistensavstemmingDataTaskDTO::class.java)
        assertThat(dataTaskDto.chunkNr).isEqualTo(1)
        assertThat(dataTaskDto.transaksjonsId).isEqualTo(transaksjonsId)
        assertThat(dataTaskDto.perioderForBehandling).hasSize(1)
        assertThat(dataTaskDto.sendTilØkonomi).isTrue()

        lagMockAvsluttHappyCase()
        val avsluttTask =
            taskSlots.find { it.type == KonsistensavstemMotOppdragAvsluttTask.TASK_STEP_TYPE }!!
        konsistensavstemMotOppdragAvsluttTask.doTask(
            Task(
                payload = avsluttTask.payload,
                type = KonsistensavstemMotOppdragAvsluttTask.TASK_STEP_TYPE
            )
        )

        verify(exactly = 1) { økonomiKlient.konsistensavstemOppdragStart(any(), transaksjonsId) }
        verify(exactly = 1) { økonomiKlient.konsistensavstemOppdragData(any(), any(), transaksjonsId) }
        verify(exactly = 1) { økonomiKlient.konsistensavstemOppdragAvslutt(any(), transaksjonsId) }
    }

    @Test
    fun `Kjør alle tasker med input generert fra task som oppretter tasken og send til økonomi skrudd av`() {
        lagMockForStartTaskHappCase()
        konistensavstemmingStartTask.doTask(
            Task(
                payload = objectMapper.writeValueAsString(konsistensavstemmingStartTaskDTO.copy(sendTilØkonomi = false)),
                type = KonsistensavstemMotOppdragStartTask.TASK_STEP_TYPE
            )
        )
        val taskSlots = mutableListOf<Task>()
        verify(exactly = 2) { taskRepository.save(capture(taskSlots)) }

        lagMockFinnPerioderForRelevanteBehandlingerHappeCase()
        val finnPerioderForRelevanteBehandlingerTask =
            taskSlots.find { it.type == KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.TASK_STEP_TYPE }!!
        konsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.doTask(
            Task(
                payload = finnPerioderForRelevanteBehandlingerTask.payload,
                type = KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.TASK_STEP_TYPE
            )
        )
        verify(exactly = 3) { taskRepository.save(capture(taskSlots)) }

        lagMockOppdragDataHappeCase()
        konsistensavstemMotOppdragDataTask.doTask(
            Task(
                payload = taskSlots.first { it.type == KonsistensavstemMotOppdragDataTask.TASK_STEP_TYPE }.payload,
                type = KonsistensavstemMotOppdragDataTask.TASK_STEP_TYPE
            )
        )
        verify(exactly = 3) { taskRepository.save(capture(taskSlots)) }
        val datachunksSlot = mutableListOf<DataChunk>()
        verify(exactly = 2) { dataChunkRepository.save(capture(datachunksSlot)) }
        assertThat(datachunksSlot.last().erSendt).isTrue()

        lagMockAvsluttHappyCase()
        val avsluttTask =
            taskSlots.find { it.type == KonsistensavstemMotOppdragAvsluttTask.TASK_STEP_TYPE }!!
        konsistensavstemMotOppdragAvsluttTask.doTask(
            Task(
                payload = avsluttTask.payload,
                type = KonsistensavstemMotOppdragAvsluttTask.TASK_STEP_TYPE
            )
        )

        verify(exactly = 0) { økonomiKlient.konsistensavstemOppdragStart(any(), transaksjonsId) }
        verify(exactly = 0) { økonomiKlient.konsistensavstemOppdragData(any(), any(), transaksjonsId) }
        verify(exactly = 0) { økonomiKlient.konsistensavstemOppdragAvslutt(any(), transaksjonsId) }
    }

    private fun lagMockForStartTaskHappCase(): CapturingSlot<LocalDateTime> {
        val behandlingId = BigInteger.ONE
        val page = mockk<Page<BigInteger>>()
        val pageable = Pageable.ofSize(KonsistensavstemMotOppdragStartTask.ANTALL_BEHANDLINGER)
        every { behandlingHentOgPersisterService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(pageable) } returns page
        every { page.totalPages } returns 1
        every { page.content } returns listOf(behandlingId)
        every { page.nextPageable() } returns pageable

        every { batchRepository.getReferenceById(batchId) } returns Batch(id = batchId, kjøreDato = LocalDate.now())
        every { dataChunkRepository.save(any()) } returns DataChunk(
            batch = Batch(kjøreDato = LocalDate.now()),
            chunkNr = 1,
            transaksjonsId = transaksjonsId
        )

        val avstemmingsdatoSlot = slot<LocalDateTime>()
        every {
            økonomiKlient.konsistensavstemOppdragStart(
                capture(avstemmingsdatoSlot),
                transaksjonsId
            )
        } returns ""

        every { dataChunkRepository.findByTransaksjonsIdAndChunkNr(transaksjonsId, any()) } returns null
        return avstemmingsdatoSlot
    }

    private fun lagMockFinnPerioderForRelevanteBehandlingerHappeCase() {
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
            ).also { it.kildeBehandlingId = behandlingId.toLong() }
        )
        val aktivFødselsnummere = mapOf(behandlingId.toLong() to "test")
        every { behandlingHentOgPersisterService.hentAktivtFødselsnummerForBehandlinger(any()) } returns aktivFødselsnummere
    }

    private fun lagMockOppdragDataHappeCase() {
        every {
            økonomiKlient.konsistensavstemOppdragData(
                any(),
                any(),
                transaksjonsId
            )
        } returns ""

        every { dataChunkRepository.findByTransaksjonsIdAndChunkNr(transaksjonsId, 1) } returns DataChunk(
            batch = Batch(kjøreDato = LocalDate.now()),
            chunkNr = 1,
            transaksjonsId = transaksjonsId
        )
        every { dataChunkRepository.save(any()) } returns DataChunk(
            batch = Batch(kjøreDato = LocalDate.now()),
            chunkNr = 1,
            transaksjonsId = transaksjonsId
        )
    }

    private fun lagMockAvsluttHappyCase() {
        every {
            økonomiKlient.konsistensavstemOppdragAvslutt(
                any(),
                transaksjonsId
            )
        } returns ""

        every { batchRepository.saveAndFlush(any()) } returns Batch(kjøreDato = LocalDate.now())
    }
}
