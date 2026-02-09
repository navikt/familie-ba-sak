package no.nav.familie.ba.sak.task

import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.AutovedtakMånedligValutajusteringService
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import java.time.YearMonth

class MånedligValutajusteringTaskTest {
    private val autovedtakMånedligValutajusteringService = mockk<AutovedtakMånedligValutajusteringService>(relaxed = true)
    private val task = MånedligValutajusteringTask(autovedtakMånedligValutajusteringService)

    @Test
    fun `doTask utfører valutajustering når nåværende måned samsvarer med måned i paýload`() {
        val currentMonth = YearMonth.now()
        val taskDto =
            MånedligValutajusteringTask.MånedligValutajusteringTaskDto(
                fagsakId = 123L,
                måned = currentMonth,
            )
        val taskPayload = jsonMapper.writeValueAsString(taskDto)
        val taskInstance = Task(type = MånedligValutajusteringTask.TASK_STEP_TYPE, payload = taskPayload)

        assertDoesNotThrow {
            task.doTask(taskInstance)
        }

        verify {
            autovedtakMånedligValutajusteringService.utførMånedligValutajustering(
                fagsakId = 123L,
                måned = currentMonth,
            )
        }
    }

    @Test
    fun `doTask logger info og utfører ikke valutajustering når nåværende måned ikke samsvarer med måned i paýload`() {
        val taskDto =
            MånedligValutajusteringTask.MånedligValutajusteringTaskDto(
                fagsakId = 123L,
                måned = YearMonth.now().minusMonths(1),
            )
        val taskPayload = jsonMapper.writeValueAsString(taskDto)
        val taskInstance = Task(type = MånedligValutajusteringTask.TASK_STEP_TYPE, payload = taskPayload)

        assertDoesNotThrow {
            task.doTask(taskInstance)
        }

        verify(exactly = 0) {
            autovedtakMånedligValutajusteringService.utførMånedligValutajustering(any(), any())
        }
    }
}
