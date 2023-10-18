package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.internal.ForvalterService
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FinnSakerMedFlereMigreringsbehandlingerTaskTest {
    private val forvalterService = mockk<ForvalterService>()
    private val task = FinnSakerMedFlereMigreringsbehandlingerTask(forvalterService)

    @Test
    fun `Skal kaste feil hvis man finner åpne saker med flere migreringsbehandlinger fra sist måned`() {
        every { forvalterService.finnÅpneFagsakerMedFlereMigreringsbehandlinger(any()) } returns
            listOf(Pair(1, "fnr1"), Pair(2, "fnr2"))

        val exception = assertThrows<IllegalStateException> {
            task.doTask(Task(type = FinnSakerMedFlereMigreringsbehandlingerTask.TASK_STEP_TYPE, payload = "2023-10"))
        }

        assertThat(exception.message).isEqualTo(
            """
            Det er nye fagsaker som har har flere enn 1 migrering fra Infotrygd. Send liste til fag og avvikshåndter tasken. fraOgMedÅrMåned=2023-10 
            (1, fnr1)
            (2, fnr2)
            """.trimIndent(),
        )
    }

    @Test
    fun `Skal ikke kaste feil hvis man ikke finner nye åpne saker som har flere enn 1 migrering`() {
        every { forvalterService.finnÅpneFagsakerMedFlereMigreringsbehandlinger(any()) } returns emptyList()

        task.doTask(Task(type = FinnSakerMedFlereMigreringsbehandlingerTask.TASK_STEP_TYPE, payload = "2023-10"))
    }
}
