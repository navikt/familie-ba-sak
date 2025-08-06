package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class AutovedtakFinnmarkstilleggTaskTest {
    private val autovedtakStegService = mockk<AutovedtakStegService>()
    private val fagsakService = mockk<FagsakService>()
    private val autovedtakFinnmarkstilleggTask =
        AutovedtakFinnmarkstilleggTask(
            autovedtakStegService = autovedtakStegService,
            fagsakService = fagsakService,
        )

    @Test
    fun `doTask skal kjøre behandling for Finnmarkstillegg med riktige parametere`() {
        // Arrange
        val fagsakId = 12345L
        val aktør = randomAktør()

        val task =
            Task(
                type = AutovedtakFinnmarkstilleggTask.TASK_STEP_TYPE,
                payload = fagsakId.toString(),
            )

        every { fagsakService.hentAktør(fagsakId) } returns aktør
        every {
            autovedtakStegService.kjørBehandlingFinnmarkstillegg(
                mottakersAktør = aktør,
                fagsakId = fagsakId,
                førstegangKjørt = any(),
            )
        } returns AutovedtakStegService.BEHANDLING_FERDIG

        // Act
        autovedtakFinnmarkstilleggTask.doTask(task)

        // Assert
        verify(exactly = 1) { fagsakService.hentAktør(fagsakId) }
        verify(exactly = 1) {
            autovedtakStegService.kjørBehandlingFinnmarkstillegg(
                mottakersAktør = aktør,
                fagsakId = fagsakId,
                førstegangKjørt = any(),
            )
        }
    }

    @Test
    fun `doTask skal håndtere ulike fagsakId verdier korrekt`() {
        // Arrange
        val fagsakId = 98765L
        val aktør = randomAktør()

        val task =
            Task(
                type = AutovedtakFinnmarkstilleggTask.TASK_STEP_TYPE,
                payload = fagsakId.toString(),
            )

        every { fagsakService.hentAktør(fagsakId) } returns aktør
        every {
            autovedtakStegService.kjørBehandlingFinnmarkstillegg(
                mottakersAktør = aktør,
                fagsakId = fagsakId,
                førstegangKjørt = any(),
            )
        } returns AutovedtakStegService.BEHANDLING_FERDIG

        // Act
        autovedtakFinnmarkstilleggTask.doTask(task)

        // Assert
        verify(exactly = 1) { fagsakService.hentAktør(fagsakId) }
        verify(exactly = 1) {
            autovedtakStegService.kjørBehandlingFinnmarkstillegg(
                mottakersAktør = aktør,
                fagsakId = fagsakId,
                førstegangKjørt = any(),
            )
        }
    }

    @Test
    fun `doTask skal kaste exception hvis payload ikke kan konverteres til Long`() {
        // Arrange
        val task =
            Task(
                type = AutovedtakFinnmarkstilleggTask.TASK_STEP_TYPE,
                payload = "ikke-et-tall",
            )

        // Act & Assert
        assertThrows<NumberFormatException> {
            autovedtakFinnmarkstilleggTask.doTask(task)
        }

        verify(exactly = 0) { fagsakService.hentAktør(any()) }
        verify(exactly = 0) { autovedtakStegService.kjørBehandlingFinnmarkstillegg(any(), any(), any()) }
    }

    @Test
    fun `doTask skal propagere exception fra fagsakService`() {
        // Arrange
        val fagsakId = 12345L
        val task =
            Task(
                type = AutovedtakFinnmarkstilleggTask.TASK_STEP_TYPE,
                payload = fagsakId.toString(),
            )

        every { fagsakService.hentAktør(fagsakId) } throws RuntimeException("Fagsak ikke funnet")

        // Act & Assert
        assertThrows<RuntimeException> {
            autovedtakFinnmarkstilleggTask.doTask(task)
        }

        verify(exactly = 1) { fagsakService.hentAktør(fagsakId) }
        verify(exactly = 0) { autovedtakStegService.kjørBehandlingFinnmarkstillegg(any(), any(), any()) }
    }

    @Test
    fun `doTask skal propagere exception fra autovedtakStegService`() {
        // Arrange
        val fagsakId = 12345L
        val aktør = randomAktør()

        val task =
            Task(
                type = AutovedtakFinnmarkstilleggTask.TASK_STEP_TYPE,
                payload = fagsakId.toString(),
            )

        every { fagsakService.hentAktør(fagsakId) } returns aktør
        every {
            autovedtakStegService.kjørBehandlingFinnmarkstillegg(
                mottakersAktør = aktør,
                fagsakId = fagsakId,
            )
        } throws RuntimeException("Feil under behandling")

        // Act & Assert
        assertThrows<RuntimeException> {
            autovedtakFinnmarkstilleggTask.doTask(task)
        }

        verify(exactly = 1) { fagsakService.hentAktør(fagsakId) }
        verify(exactly = 1) {
            autovedtakStegService.kjørBehandlingFinnmarkstillegg(
                mottakersAktør = aktør,
                fagsakId = fagsakId,
                førstegangKjørt = any(),
            )
        }
    }

    @Test
    fun `doTask skal håndtere tom payload som ugyldig`() {
        // Arrange
        val task =
            Task(
                type = AutovedtakFinnmarkstilleggTask.TASK_STEP_TYPE,
                payload = "",
            )

        // Act & Assert
        assertThrows<NumberFormatException> {
            autovedtakFinnmarkstilleggTask.doTask(task)
        }

        verify(exactly = 0) { fagsakService.hentAktør(any()) }
        verify(exactly = 0) { autovedtakStegService.kjørBehandlingFinnmarkstillegg(any(), any(), any()) }
    }
}
