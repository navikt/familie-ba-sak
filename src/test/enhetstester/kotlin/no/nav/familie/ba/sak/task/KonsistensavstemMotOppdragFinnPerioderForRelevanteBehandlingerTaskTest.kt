package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.økonomi.AvstemmingService
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingDataTaskDTO
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.PerioderForBehandling
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTaskTest {
    private val avstemmingService = mockk<AvstemmingService>()
    private val taskService = mockk<TaskService>()
    private val featureToggleService = mockk<FeatureToggleService>()

    private val konsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask =
        KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask(
            avstemmingService = avstemmingService,
            taskService = taskService,
            featureToggleService = featureToggleService,
        )

    @Test
    fun `skal returnere dersom vi har kjørt konsistensavstemming for transaksjonsId og chunk tidligere`() {
        // Arrange
        val konsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO =
            KonsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO(
                batchId = 1,
                transaksjonsId = UUID.randomUUID(),
                avstemmingsdato = LocalDateTime.now(),
                chunkNr = 1,
                relevanteBehandlinger = listOf(1, 2, 3),
                sendTilØkonomi = true,
            )

        val task = Task(type = KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.TASK_STEP_TYPE, payload = objectMapper.writeValueAsString(konsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO))

        every { avstemmingService.erKonsistensavstemmingKjørtForTransaksjonsidOgChunk(konsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO.transaksjonsId, konsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO.chunkNr) } returns true

        // Act
        konsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.doTask(task)

        // Assert
        verify(exactly = 0) { taskService.save(any()) }
    }

    @Test
    fun `skal finne perioder til avstemming og opprette KonsistensavstemMotOppdragDataTask når toggle om bruk av utbetalingstidslinjer er av`() {
        // Arrange
        val taskDto =
            KonsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO(
                batchId = 1,
                transaksjonsId = UUID.randomUUID(),
                avstemmingsdato = LocalDateTime.now(),
                chunkNr = 1,
                relevanteBehandlinger = listOf(1, 2, 3),
                sendTilØkonomi = true,
            )

        val task = Task(type = KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.TASK_STEP_TYPE, payload = objectMapper.writeValueAsString(taskDto))

        every {
            avstemmingService.erKonsistensavstemmingKjørtForTransaksjonsidOgChunk(
                transaksjonsId = taskDto.transaksjonsId,
                chunkNr = taskDto.chunkNr,
            )
        } returns false

        every { featureToggleService.isEnabled(FeatureToggle.BRUK_UTBETALINGSTIDSLINJER_VED_GENERERING_AV_PERIODER_TIL_AVSTEMMING, false) } returns false

        val perioderTilAvstemming =
            listOf(
                PerioderForBehandling(
                    behandlingId = "1",
                    perioder = setOf(0, 1, 2),
                    aktivFødselsnummer = "12345678910",
                    utebetalesTil = "12345",
                ),
            )
        every {
            avstemmingService.hentDataForKonsistensavstemming(
                avstemmingstidspunkt = taskDto.avstemmingsdato,
                relevanteBehandlinger = taskDto.relevanteBehandlinger,
            )
        } returns perioderTilAvstemming

        val taskCapturingSlot = slot<Task>()
        every { taskService.save(capture(taskCapturingSlot)) } answers { firstArg() }

        // Act
        konsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.doTask(task)

        // Assert
        verify(exactly = 1) { taskService.save(any()) }
        val lagretTask = taskCapturingSlot.captured

        assertThat(lagretTask.metadata["chunkNr"]).isEqualTo(taskDto.chunkNr.toString())
        assertThat(lagretTask.metadata["transaksjonsId"]).isEqualTo(taskDto.transaksjonsId.toString())

        val konsistensavstemmingDataTaskDTO = objectMapper.readValue(lagretTask.payload, KonsistensavstemmingDataTaskDTO::class.java)

        assertThat(konsistensavstemmingDataTaskDTO.perioderForBehandling).isEqualTo(perioderTilAvstemming)
        assertThat(konsistensavstemmingDataTaskDTO.chunkNr).isEqualTo(taskDto.chunkNr)
        assertThat(konsistensavstemmingDataTaskDTO.transaksjonsId).isEqualTo(taskDto.transaksjonsId)
        assertThat(konsistensavstemmingDataTaskDTO.avstemmingdato).isEqualTo(taskDto.avstemmingsdato)
        assertThat(konsistensavstemmingDataTaskDTO.sendTilØkonomi).isEqualTo(taskDto.sendTilØkonomi)
    }

    @Test
    fun `skal finne perioder til avstemming og opprette KonsistensavstemMotOppdragDataTask når toggle om bruk av utbetalingstidslinjer er på`() {
        // Arrange
        val taskDto =
            KonsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO(
                batchId = 1,
                transaksjonsId = UUID.randomUUID(),
                avstemmingsdato = LocalDateTime.now(),
                chunkNr = 1,
                relevanteBehandlinger = listOf(1, 2, 3),
                sendTilØkonomi = true,
            )

        val task = Task(type = KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.TASK_STEP_TYPE, payload = objectMapper.writeValueAsString(taskDto))

        every {
            avstemmingService.erKonsistensavstemmingKjørtForTransaksjonsidOgChunk(
                transaksjonsId = taskDto.transaksjonsId,
                chunkNr = taskDto.chunkNr,
            )
        } returns false

        every { featureToggleService.isEnabled(FeatureToggle.BRUK_UTBETALINGSTIDSLINJER_VED_GENERERING_AV_PERIODER_TIL_AVSTEMMING, false) } returns true

        val perioderTilAvstemming =
            listOf(
                PerioderForBehandling(
                    behandlingId = "1",
                    perioder = setOf(0, 1, 2),
                    aktivFødselsnummer = "12345678910",
                    utebetalesTil = "12345",
                ),
            )
        every {
            avstemmingService.hentDataForKonsistensavstemmingVedHjelpAvUtbetalingstidslinjer(
                avstemmingstidspunkt = taskDto.avstemmingsdato,
                relevanteBehandlinger = taskDto.relevanteBehandlinger,
            )
        } returns perioderTilAvstemming

        val taskCapturingSlot = slot<Task>()
        every { taskService.save(capture(taskCapturingSlot)) } answers { firstArg() }

        // Act
        konsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.doTask(task)

        // Assert
        verify(exactly = 1) { taskService.save(any()) }
        val lagretTask = taskCapturingSlot.captured

        assertThat(lagretTask.metadata["chunkNr"]).isEqualTo(taskDto.chunkNr.toString())
        assertThat(lagretTask.metadata["transaksjonsId"]).isEqualTo(taskDto.transaksjonsId.toString())

        val konsistensavstemmingDataTaskDTO = objectMapper.readValue(lagretTask.payload, KonsistensavstemmingDataTaskDTO::class.java)

        assertThat(konsistensavstemmingDataTaskDTO.perioderForBehandling).isEqualTo(perioderTilAvstemming)
        assertThat(konsistensavstemmingDataTaskDTO.chunkNr).isEqualTo(taskDto.chunkNr)
        assertThat(konsistensavstemmingDataTaskDTO.transaksjonsId).isEqualTo(taskDto.transaksjonsId)
        assertThat(konsistensavstemmingDataTaskDTO.avstemmingdato).isEqualTo(taskDto.avstemmingsdato)
        assertThat(konsistensavstemmingDataTaskDTO.sendTilØkonomi).isEqualTo(taskDto.sendTilØkonomi)
    }
}
