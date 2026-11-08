package no.nav.familie.ba.sak.integrasjoner.økonomi

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class KonsistensavstemmingControllerTest {
    private val taskService = mockk<TaskService>()
    private val batchRepository = mockk<BatchRepository>()
    private val tilgangService = mockk<TilgangService>()

    private val konsistensavstemmingController =
        KonsistensavstemmingController(
            taskService = taskService,
            batchRepository = batchRepository,
            tilgangService = tilgangService,
        )

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.verifiserHarTilgangTilHandling(any(), any()) }
        every { batchRepository.saveAndFlush(any()) } answers { firstArg() }
        every { taskService.save(any()) } answers { firstArg() }
    }

    @Nested
    inner class KjørKonsistensavstemmingUtenSendingTilØkonomi {
        @Test
        fun `skal kreve forvalterrolle`() {
            // Act
            konsistensavstemmingController.kjørKonsistensavstemmingUtenSendingTilØkonomi()

            // Assert
            verify(exactly = 1) {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.FORVALTER,
                    handling = "Testkjøre konsistensavstemming uten sending til økonomi",
                )
            }
        }

        @Test
        fun `skal ikke opprette task når kaller ikke er forvalter`() {
            // Arrange
            every {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.FORVALTER,
                    handling = "Testkjøre konsistensavstemming uten sending til økonomi",
                )
            } throws RolleTilgangskontrollFeil("Krever FORVALTER")

            // Act & Assert
            val feilmelding =
                assertThrows<RolleTilgangskontrollFeil> {
                    konsistensavstemmingController.kjørKonsistensavstemmingUtenSendingTilØkonomi()
                }.message

            assertThat(feilmelding).isEqualTo("Krever FORVALTER")
            verify(exactly = 0) { batchRepository.saveAndFlush(any()) }
            verify(exactly = 0) { taskService.save(any()) }
        }
    }

    @Nested
    inner class KjørKonsistensavstemming {
        private val request = KonsistensavstemmingController.StartKonsistensavstemming(triggerTid = LocalDateTime.now())

        @Test
        fun `skal kreve forvalterrolle`() {
            // Act
            konsistensavstemmingController.kjørKonsistensavstemming(request)

            // Assert
            verify(exactly = 1) {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.FORVALTER,
                    handling = "Kjøre konsistensavstemming",
                )
            }
        }

        @Test
        fun `skal ikke opprette task når kaller ikke er forvalter`() {
            // Arrange
            every {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.FORVALTER,
                    handling = "Kjøre konsistensavstemming",
                )
            } throws RolleTilgangskontrollFeil("Krever FORVALTER")

            // Act & Assert
            val feilmelding =
                assertThrows<RolleTilgangskontrollFeil> {
                    konsistensavstemmingController.kjørKonsistensavstemming(request)
                }.message

            assertThat(feilmelding).isEqualTo("Krever FORVALTER")
            verify(exactly = 0) { batchRepository.saveAndFlush(any()) }
            verify(exactly = 0) { taskService.save(any()) }
        }
    }
}
