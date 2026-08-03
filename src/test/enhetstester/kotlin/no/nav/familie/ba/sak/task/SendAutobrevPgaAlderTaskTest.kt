package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.AutobrevOmregningPgaAlderService
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.AutobrevOmregningSvar
import no.nav.familie.ba.sak.task.dto.AutobrevPgaAlderDTO
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import java.time.YearMonth

class SendAutobrevPgaAlderTaskTest {
    private val autobrevOmregningPgaAlderService = mockk<AutobrevOmregningPgaAlderService>(relaxed = true)
    private val sendAutobrevPgaAlderTask = SendAutobrevPgaAlderTask(autobrevOmregningPgaAlderService)

    @Test
    fun `ignorere gammel task når nåværende måned ikke samsvarer med måned i payload`() {
        // Arrange
        val autobrevDTO = AutobrevPgaAlderDTO(fagsakId = 1, alder = 18, årMåned = YearMonth.now().minusMonths(1))
        val taskPayload = jsonMapper.writeValueAsString(autobrevDTO)
        val taskInstance = Task(type = SendAutobrevPgaAlderTask.TASK_STEP_TYPE, payload = taskPayload)

        // Act
        assertDoesNotThrow {
            sendAutobrevPgaAlderTask.doTask(taskInstance)
        }

        // Assert
        verify(exactly = 0) {
            autobrevOmregningPgaAlderService.opprettOmregningsoppgaveForBarnIBrytingsalder(any(), any())
        }
        assert(taskInstance.metadata["resultat"] == "Ignorerer task, da den ikke kjøres i riktig måned")
    }

    @Test
    fun `Kall opprettOmregningsoppgaveForBarnIBrytingsalder hvis dato i tasken er nåværendeMåned måned`() {
        // Arrange
        val nåværendeMåned = YearMonth.now()
        val autobrevDTO = AutobrevPgaAlderDTO(fagsakId = 1, alder = 18, årMåned = nåværendeMåned)
        val taskPayload = jsonMapper.writeValueAsString(autobrevDTO)
        val taskInstance = Task(type = SendAutobrevPgaAlderTask.TASK_STEP_TYPE, payload = taskPayload)

        every {
            autobrevOmregningPgaAlderService.opprettOmregningsoppgaveForBarnIBrytingsalder(any(), any())
        } returns AutobrevOmregningSvar.OK

        // Act
        assertDoesNotThrow {
            sendAutobrevPgaAlderTask.doTask(taskInstance)
        }

        // Assert
        verify {
            autobrevOmregningPgaAlderService.opprettOmregningsoppgaveForBarnIBrytingsalder(any(), any())
        }
        assert(taskInstance.metadata["resultat"] == "OK")
    }
}
