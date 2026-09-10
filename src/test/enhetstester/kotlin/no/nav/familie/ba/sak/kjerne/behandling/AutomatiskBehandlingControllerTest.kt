package no.nav.familie.ba.sak.kjerne.behandling

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.AuditLoggerEvent
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
            søkersIdent = "12345678910",
            barnasIdenter = listOf("12345678911"),
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
        fun `skal validere tilgang til fagsak`() {
            // Act
            automatiskBehandlingController.opprettAutomatiskBehandlingAvSøknad(nyBehandling)

            // Assert
            verify(exactly = 1) {
                tilgangService.validerTilgangTilFagsak(
                    fagsakId = nyBehandling.fagsakId,
                    event = AuditLoggerEvent.CREATE,
                )
            }
        }

        @Test
        fun `skal verifisere at saksbehandler har riktig rolle`() {
            // Act
            automatiskBehandlingController.opprettAutomatiskBehandlingAvSøknad(nyBehandling)

            // Assert
            verify(exactly = 1) {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                    handling = "opprette behandling",
                )
            }
        }

        @Test
        fun `skal kaste feil hvis saksbehandler ikke har tilgang til fagsak`() {
            // Arrange
            every {
                tilgangService.validerTilgangTilFagsak(
                    fagsakId = nyBehandling.fagsakId,
                    event = AuditLoggerEvent.CREATE,
                )
            } throws RolleTilgangskontrollFeil("Ikke tilgang")

            // Act & Assert
            val feilmelding =
                assertThrows<RolleTilgangskontrollFeil> {
                    automatiskBehandlingController.opprettAutomatiskBehandlingAvSøknad(nyBehandling)
                }.message

            assertThat(feilmelding).isEqualTo("Ikke tilgang")

            verify(exactly = 0) { taskRepository.save(any()) }
        }

        @Test
        fun `skal kaste feil hvis saksbehandler ikke har riktig rolle`() {
            // Arrange
            every {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                    handling = "opprette behandling",
                )
            } throws RolleTilgangskontrollFeil("Du har ikke saksbehandler-tilgang.")

            // Act & Assert
            val feilmelding =
                assertThrows<RolleTilgangskontrollFeil> {
                    automatiskBehandlingController.opprettAutomatiskBehandlingAvSøknad(nyBehandling)
                }.message

            assertThat(feilmelding).isEqualTo("Du har ikke saksbehandler-tilgang.")

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
