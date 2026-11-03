package no.nav.familie.ba.sak.statistikk.stønadsstatistikk

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.eksterne.kontrakter.VedtakDVHV2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StønadsstatistikkControllerTest {
    private val stønadsstatistikkService = mockk<StønadsstatistikkService>()
    private val taskRepository = mockk<TaskRepositoryWrapper>()
    private val tilgangService = mockk<TilgangService>()

    private val stønadsstatistikkController =
        StønadsstatistikkController(
            stønadsstatistikkService = stønadsstatistikkService,
            taskRepository = taskRepository,
            tilgangService = tilgangService,
        )

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.verifiserHarTilgangTilHandling(any(), any()) }
        every { stønadsstatistikkService.hentVedtakV2(any()) } returns mockk<VedtakDVHV2>(relaxed = true)
        every { taskRepository.save(any()) } returns mockk()
    }

    @Nested
    inner class HentVedtakDvhV2 {
        @Test
        fun `skal kreve forvalterrolle`() {
            // Act
            stønadsstatistikkController.hentVedtakDvhV2(listOf(1L))

            // Assert
            verify(exactly = 1) {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.FORVALTER,
                    handling = "Hente Vedtak DVH",
                )
            }
        }

        @Test
        fun `skal ikke hente vedtak når kaller ikke er forvalter`() {
            // Arrange
            every {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.FORVALTER,
                    handling = "Hente Vedtak DVH",
                )
            } throws RolleTilgangskontrollFeil("Krever FORVALTER")

            // Act & Assert
            val feilmelding =
                assertThrows<RolleTilgangskontrollFeil> {
                    stønadsstatistikkController.hentVedtakDvhV2(listOf(1L))
                }.message

            assertThat(feilmelding).isEqualTo("Krever FORVALTER")
            verify(exactly = 0) { stønadsstatistikkService.hentVedtakV2(any()) }
        }
    }

    @Nested
    inner class SendTilStønadsstatistikkManuell {
        @Test
        fun `skal kreve forvalterrolle`() {
            // Act
            stønadsstatistikkController.sendTilStønadsstatistikkManuell(listOf(1L))

            // Assert
            verify(exactly = 1) {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.FORVALTER,
                    handling = "Sender vedtakDVH til stønadsstatistikk manuelt",
                )
            }
        }

        @Test
        fun `skal ikke hente vedtak eller lagre task når kaller ikke er forvalter`() {
            // Arrange
            every {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.FORVALTER,
                    handling = "Sender vedtakDVH til stønadsstatistikk manuelt",
                )
            } throws RolleTilgangskontrollFeil("Krever FORVALTER")

            // Act & Assert
            val feilmelding =
                assertThrows<RolleTilgangskontrollFeil> {
                    stønadsstatistikkController.sendTilStønadsstatistikkManuell(listOf(1L))
                }.message

            assertThat(feilmelding).isEqualTo("Krever FORVALTER")
            verify(exactly = 0) { stønadsstatistikkService.hentVedtakV2(any()) }
            verify(exactly = 0) { taskRepository.save(any()) }
        }
    }
}
