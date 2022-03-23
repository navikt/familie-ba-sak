package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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
                avstemmingdato = avstemmingdato,
            )
        )
        val task = Task(payload = payload, type = KonsistensavstemMotOppdragStartTask.TASK_STEP_TYPE)
        val startTask = KonsistensavstemMotOppdragStartTask(avstemmingService)

        every { avstemmingService.nullstillDataChunk() } just runs
        every { avstemmingService.sendKonsistensavstemmingStart(avstemmingdato, any()) } just runs
        val page = mockk<Page<BigInteger>>()
        val pageable = mockk<Pageable>()
        val nrOfPages = 3
        every { page.totalPages } returns nrOfPages
        every { page.nextPageable() } returns pageable
        every { avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(any()) } returns page
        every {
            avstemmingService.opprettKonsistensavstemmingDataTask(
                avstemmingdato,
                any(),
                batchId,
                any(),
                any()
            )
        } just runs
        every { avstemmingService.opprettKonsistensavstemmingAvsluttTask(batchId, any(), avstemmingdato) } just runs
        startTask.doTask(task)

        verify(exactly = 1) { avstemmingService.nullstillDataChunk() }
        verify(exactly = 1) { avstemmingService.sendKonsistensavstemmingStart(avstemmingdato, any()) }
        verify(exactly = nrOfPages) {
            avstemmingService.opprettKonsistensavstemmingDataTask(
                avstemmingdato,
                any(),
                batchId,
                any(),
                any()
            )
        }
    }
}
