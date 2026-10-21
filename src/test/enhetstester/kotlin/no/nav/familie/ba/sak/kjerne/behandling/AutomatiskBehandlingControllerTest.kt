package no.nav.familie.ba.sak.kjerne.behandling

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.søknad.`AutovedtakSøknadFeilårsak`
import no.nav.familie.ba.sak.kjerne.autovedtak.søknad.`AutovedtakSøknadStatistikkDto`
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.statistikk.automatiskbehandlingavSoknadStatistikk.AutovedtakSøknadStatistikkService
import no.nav.familie.ba.sak.task.BehandleAutomatiskSøknadTask
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class AutomatiskBehandlingControllerTest {
    private val tilgangService = mockk<TilgangService>()
    private val taskRepository = mockk<TaskRepositoryWrapper>()
    private val statistikkService = mockk<AutovedtakSøknadStatistikkService>()

    private val automatiskBehandlingController =
        AutomatiskBehandlingController(
            tilgangService = tilgangService,
            taskRepository = taskRepository,
            autovedtakSøknadStatistikkService = statistikkService,
        )

    private val nyBehandling =
        NyBehandling(
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            fagsakId = 1L,
            barnasIdenter = listOf("12345678911"),
            søknadMottattDato = LocalDate.now(),
        )

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilFagsak(any(), any()) }
        justRun { tilgangService.verifiserHarTilgangTilHandling(any(), any()) }
        every { taskRepository.save(any()) } returns mockk()
    }

    @Nested
    inner class OpprettAutomatiskBehandlingAvSøknad {
        @Test
        fun `skal verifisere at kaller av endepunktet har riktig rolle`() {
            // Act
            automatiskBehandlingController.opprettAutomatiskBehandlingAvSøknad(nyBehandling)

            // Assert
            verify(exactly = 1) {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.SYSTEM,
                    handling = "Oppretter behandling fra søknad",
                )
            }
        }

        @Test
        fun `skal kaste feil kaller av endepunktet ikke har riktig rolle`() {
            // Arrange
            every {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.SYSTEM,
                    handling = "Oppretter behandling fra søknad",
                )
            } throws RolleTilgangskontrollFeil("Du har ikke system-tilgang.")

            // Act & Assert
            val feilmelding =
                assertThrows<RolleTilgangskontrollFeil> {
                    automatiskBehandlingController.opprettAutomatiskBehandlingAvSøknad(nyBehandling)
                }.message

            assertThat(feilmelding).isEqualTo("Du har ikke system-tilgang.")

            verify(exactly = 0) { taskRepository.save(any()) }
        }

        @Test
        fun `skal opprette og lagre en task for automatisk behandling av søknad`() {
            // Arrange
            val taskSlot = slot<Task>()
            every { taskRepository.save(capture(taskSlot)) } returns mockk()

            // Act
            automatiskBehandlingController.opprettAutomatiskBehandlingAvSøknad(nyBehandling)

            // Assert
            verify(exactly = 1) { taskRepository.save(any()) }
            assertThat(taskSlot.captured.type).isEqualTo(BehandleAutomatiskSøknadTask.TASK_STEP_TYPE)
        }
    }

    @Nested
    inner class RegistrerStatistikkForAutovedtakSøknad {
        private val årsaker =
            listOf(
                AutovedtakSøknadFeilårsak.SØKER_HAR_LØPENDE_BARNETRYGD,
                AutovedtakSøknadFeilårsak.SØKNAD_MANGLER_VEDLEGG,
                AutovedtakSøknadFeilårsak.SØKER_HAR_HATT_BARNETRYGD_TIDLIGERE,
            )
        private val statistikkDto = AutovedtakSøknadStatistikkDto(journalpostId = 123L, feilårsaker = årsaker)

        @BeforeEach
        fun setUp() {
            justRun { statistikkService.lagreÅrsakerForJournalpost(any(), any()) }
        }

        @Test
        fun `skal verifisere at kaller av endepunktet har riktig rolle`() {
            // Act
            automatiskBehandlingController.registrerStatistikkForAutovedtakSøknad(statistikkDto)

            // Assert
            verify(exactly = 1) {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.SYSTEM,
                    handling = "Registrerer årsaker til at søknad ikke behandles automatisk",
                )
            }
        }

        @Test
        fun `skal kaste feil når kaller av endepunktet ikke har riktig rolle`() {
            // Arrange
            every {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.SYSTEM,
                    handling = "Registrerer årsaker til at søknad ikke behandles automatisk",
                )
            } throws RolleTilgangskontrollFeil("Du har ikke system-tilgang.")

            // Act & Assert
            val feilmelding =
                assertThrows<RolleTilgangskontrollFeil> {
                    automatiskBehandlingController.registrerStatistikkForAutovedtakSøknad(statistikkDto)
                }.message

            assertThat(feilmelding).isEqualTo("Du har ikke system-tilgang.")
            verify(exactly = 0) { statistikkService.lagreÅrsakerForJournalpost(any(), any()) }
        }

        @Test
        fun `skal lagre årsaker fra mottak`() {
            // Act
            automatiskBehandlingController.registrerStatistikkForAutovedtakSøknad(statistikkDto)

            // Assert
            verify(exactly = 1) { statistikkService.lagreÅrsakerForJournalpost(123L, årsaker) }
        }
    }
}
