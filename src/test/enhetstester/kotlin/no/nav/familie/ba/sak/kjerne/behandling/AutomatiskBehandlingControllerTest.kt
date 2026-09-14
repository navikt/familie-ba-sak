package no.nav.familie.ba.sak.kjerne.behandling

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.sikkerhet.TilgangService
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

    private val automatiskBehandlingController =
        AutomatiskBehandlingController(
            tilgangService = tilgangService,
            taskRepository = taskRepository,
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
}
