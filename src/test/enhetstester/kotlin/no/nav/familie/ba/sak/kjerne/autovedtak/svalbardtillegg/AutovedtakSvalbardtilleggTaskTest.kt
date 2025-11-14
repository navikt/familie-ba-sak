package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.AutovedtakSkalIkkeGjennomføresFeil
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class AutovedtakSvalbardtilleggTaskTest {
    private val autovedtakStegService = mockk<AutovedtakStegService>()
    private val fagsakService = mockk<FagsakService>()
    private val opprettTaskService = mockk<OpprettTaskService>()
    private val autovedtakSvalbardtilleggTask =
        AutovedtakSvalbardtilleggTask(
            autovedtakStegService = autovedtakStegService,
            fagsakService = fagsakService,
            opprettTaskService = opprettTaskService,
        )

    @Nested
    inner class DoTask {
        @Test
        fun `doTask skal kjøre behandling for Svalbardtillegg med riktige parametere`() {
            // Arrage
            val fagsakId = 12345L
            val aktør = randomAktør()

            val task = Task(type = AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE, payload = fagsakId.toString())

            every { fagsakService.hentAktør(fagsakId) } returns aktør
            every { autovedtakStegService.kjørBehandlingSvalbardtillegg(mottakersAktør = aktør, fagsakId = fagsakId, førstegangKjørt = any()) } returns AutovedtakStegService.BEHANDLING_FERDIG

            // Act
            autovedtakSvalbardtilleggTask.doTask(task)

            // Assert
            verify(exactly = 1) { fagsakService.hentAktør(fagsakId) }
            verify(exactly = 1) { autovedtakStegService.kjørBehandlingSvalbardtillegg(mottakersAktør = aktør, fagsakId = fagsakId, førstegangKjørt = any()) }
        }

        @Test
        fun `doTask skal håndtere ulike fagsakId verdier korrekt`() {
            // Arrange
            val fagsakId = 98765L
            val aktør = randomAktør()

            val task = Task(type = AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE, payload = fagsakId.toString())

            every { fagsakService.hentAktør(fagsakId) } returns aktør
            every { autovedtakStegService.kjørBehandlingSvalbardtillegg(mottakersAktør = aktør, fagsakId = fagsakId, førstegangKjørt = any()) } returns AutovedtakStegService.BEHANDLING_FERDIG

            // Act
            autovedtakSvalbardtilleggTask.doTask(task)

            // Assert
            verify(exactly = 1) { fagsakService.hentAktør(fagsakId) }
            verify(exactly = 1) { autovedtakStegService.kjørBehandlingSvalbardtillegg(mottakersAktør = aktør, fagsakId = fagsakId, førstegangKjørt = any()) }
        }

        @Test
        fun `doTask skal kaste exception feil hvis payload ikke kan konverteres til Long`() {
            // Arrange
            val task = Task(type = AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE, payload = "ikke et tall")

            // Act and Assert
            assertThrows<NumberFormatException> {
                autovedtakSvalbardtilleggTask.doTask(task)
            }

            verify(exactly = 0) { fagsakService.hentAktør(any()) }
            verify(exactly = 0) { autovedtakStegService.kjørBehandlingSvalbardtillegg(any(), any(), any()) }
        }

        @Test
        fun `doTaskl skal propagere exception fra fagsakService`() {
            // Arrange
            val fagsakId = 12345L
            val task = Task(type = AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE, payload = fagsakId.toString())

            every { fagsakService.hentAktør(fagsakId) } throws RuntimeException("Fagsak ikke funnet")

            // Act and Assert
            assertThrows<RuntimeException> { autovedtakSvalbardtilleggTask.doTask(task) }

            verify(exactly = 1) { fagsakService.hentAktør(fagsakId) }
            verify(exactly = 0) { autovedtakStegService.kjørBehandlingSvalbardtillegg(any(), any(), any()) }
        }

        @Test
        fun `doTask skal propagere exception fra autovedtakStegService`() {
            // Arrange
            val fagsakId = 12345L
            val aktør = randomAktør()

            val task = Task(type = AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE, payload = fagsakId.toString())

            every { fagsakService.hentAktør(fagsakId) } returns aktør
            every { autovedtakStegService.kjørBehandlingSvalbardtillegg(mottakersAktør = aktør, fagsakId = fagsakId, førstegangKjørt = any()) } throws RuntimeException("Feil under behanlding")

            // Act and Assert
            assertThrows<RuntimeException> { autovedtakSvalbardtilleggTask.doTask(task) }

            verify(exactly = 1) { fagsakService.hentAktør(fagsakId) }
            verify(exactly = 1) { autovedtakStegService.kjørBehandlingSvalbardtillegg(mottakersAktør = aktør, fagsakId = fagsakId, førstegangKjørt = any()) }
        }

        @Test
        fun `doTask skal håndtere tom payload som ugyldig`() {
            // Arrange
            val task = Task(type = AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE, payload = "")

            // Act and Assert
            assertThrows<NumberFormatException> { autovedtakSvalbardtilleggTask.doTask(task) }

            verify(exactly = 0) { fagsakService.hentAktør(any()) }
            verify(exactly = 0) { autovedtakStegService.kjørBehandlingSvalbardtillegg(any(), any(), any()) }
        }

        @Test
        fun `doTask skal håndtere AutovedtakSkalIkkeGjennomføresFeil riktig`() {
            // Arrange
            val fagsakId = 12345L
            val aktør = randomAktør()

            val task =
                Task(
                    type = AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE,
                    payload = fagsakId.toString(),
                )

            every { fagsakService.hentAktør(fagsakId) } returns aktør
            every {
                autovedtakStegService.kjørBehandlingSvalbardtillegg(
                    mottakersAktør = aktør,
                    fagsakId = fagsakId,
                    førstegangKjørt = any(),
                )
            } throws AutovedtakSkalIkkeGjennomføresFeil("Feilmelding")

            // Act
            assertDoesNotThrow { autovedtakSvalbardtilleggTask.doTask(task) }

            // Assert
            verify(exactly = 1) { fagsakService.hentAktør(fagsakId) }
            verify(exactly = 1) {
                autovedtakStegService.kjørBehandlingSvalbardtillegg(
                    mottakersAktør = aktør,
                    fagsakId = fagsakId,
                    førstegangKjørt = any(),
                )
            }
        }

        @Test
        fun `doTask skal håndtere AutovedtakMåBehandlesManueltFeil riktig`() {
            // Arrange
            val fagsakId = 12345L
            val aktør = randomAktør()

            val task =
                Task(
                    type = AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE,
                    payload = fagsakId.toString(),
                )

            every { fagsakService.hentAktør(fagsakId) } returns aktør
            justRun { opprettTaskService.opprettOppgaveForFinnmarksOgSvalbardtilleggTask(fagsakId, any()) }
            every {
                autovedtakStegService.kjørBehandlingSvalbardtillegg(
                    mottakersAktør = aktør,
                    fagsakId = fagsakId,
                    førstegangKjørt = any(),
                )
            } throws AutovedtakMåBehandlesManueltFeil("Feilmelding")

            // Act
            assertDoesNotThrow { autovedtakSvalbardtilleggTask.doTask(task) }

            // Assert
            verify(exactly = 1) { fagsakService.hentAktør(fagsakId) }
            verify(exactly = 1) {
                autovedtakStegService.kjørBehandlingSvalbardtillegg(
                    mottakersAktør = aktør,
                    fagsakId = fagsakId,
                    førstegangKjørt = any(),
                )
            }
            verify(exactly = 1) {
                opprettTaskService.opprettOppgaveForFinnmarksOgSvalbardtilleggTask(
                    fagsakId = fagsakId,
                    beskrivelse = "Feilmelding",
                )
            }
        }

        @Test
        fun `doTask skal sette riktig 'resultat' i metadata`() {
            // Arrange
            val fagsakId = 12345L
            val aktør = randomAktør()

            val task =
                Task(
                    type = AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE,
                    payload = fagsakId.toString(),
                )

            every { fagsakService.hentAktør(fagsakId) } returns aktør
            every {
                autovedtakStegService.kjørBehandlingSvalbardtillegg(any(), any(), any())
            } returns "Resultat\nmed\nlinjeskift"

            // Act
            autovedtakSvalbardtilleggTask.doTask(task)

            // Assert
            assertThat(task.metadata["resultat"]).isEqualTo("Resultat med linjeskift")
        }
    }
}
