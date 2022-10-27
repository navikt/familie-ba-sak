package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.integrasjoner.økonomi.AvstemmingService
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingStartTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigInteger
import java.time.LocalDateTime

internal class KonsistensavstemMotOppdragStartTaskTest {
    private val avstemmingService = mockk<AvstemmingService>()

    @Test
    fun `Test at start konsistensavstemmingstask nullstiller chunktabell, sender start data og avslutt task`() {
        val avstemmingdato = LocalDateTime.of(2022, 4, 1, 0, 0)
        val batchId = 123L
        val payload = objectMapper.writeValueAsString(
            KonsistensavstemmingStartTaskDTO(
                batchId = batchId,
                avstemmingdato = avstemmingdato
            )
        )
        val task = Task(payload = payload, type = KonsistensavstemMotOppdragStartTask.TASK_STEP_TYPE)
        val startTask = KonsistensavstemMotOppdragStartTask(avstemmingService)

        justRun { avstemmingService.nullstillDataChunk() }
        justRun { avstemmingService.sendKonsistensavstemmingStart(any(), any()) }
        val page = mockk<Page<BigInteger>>()
        val pageable = mockk<Pageable>()
        val nrOfPages = 3
        every { page.totalPages } returns nrOfPages
        every { page.nextPageable() } returns pageable
        every { page.content } returns listOf(BigInteger.valueOf(42))
        every { avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(any()) } returns page
        justRun {
            avstemmingService.opprettKonsistensavstemmingPerioderGeneratorTask(
                any(),
                any(),
                batchId,
                any(),
                any()
            )
        }
        justRun { avstemmingService.opprettKonsistensavstemmingAvsluttTask(batchId, any(), any()) }
        startTask.doTask(task)

        verify(exactly = 1) { avstemmingService.nullstillDataChunk() }
        verify(exactly = 1) { avstemmingService.sendKonsistensavstemmingStart(any(), any()) }
        verify(exactly = nrOfPages) {
            avstemmingService.opprettKonsistensavstemmingPerioderGeneratorTask(
                any(),
                any(),
                batchId,
                any(),
                any()
            )
        }
    }
}
