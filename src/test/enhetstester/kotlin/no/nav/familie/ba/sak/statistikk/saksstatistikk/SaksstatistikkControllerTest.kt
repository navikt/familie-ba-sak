package no.nav.familie.ba.sak.statistikk.saksstatistikk

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SaksstatistikkControllerTest {
    private val saksstatistikkService = mockk<SaksstatistikkService>()
    private val tilgangService = mockk<TilgangService>()

    private val saksstatistikkController =
        SaksstatistikkController(
            saksstatistikkService = saksstatistikkService,
            tilgangService = tilgangService,
        )

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.verifiserHarTilgangTilHandling(any(), any()) }
        every { saksstatistikkService.mapTilBehandlingDVH(any()) } returns mockk(relaxed = true)
        every { saksstatistikkService.mapTilSakDvh(any()) } returns mockk(relaxed = true)
    }

    @Nested
    inner class HentBehandlingDvh {
        @Test
        fun `skal kreve forvalterrolle`() {
            // Act
            saksstatistikkController.hentBehandlingDvh(1L)

            // Assert
            verify(exactly = 1) {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.FORVALTER,
                    handling = "Hente saksstatistikk for behandling",
                )
            }
        }

        @Test
        fun `skal ikke hente saksstatistikk for behandling når kaller ikke er forvalter`() {
            // Arrange
            every {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.FORVALTER,
                    handling = "Hente saksstatistikk for behandling",
                )
            } throws RolleTilgangskontrollFeil("Krever FORVALTER")

            // Act & Assert
            val feilmelding =
                assertThrows<RolleTilgangskontrollFeil> {
                    saksstatistikkController.hentBehandlingDvh(1L)
                }.message

            assertThat(feilmelding).isEqualTo("Krever FORVALTER")
            verify(exactly = 0) { saksstatistikkService.mapTilBehandlingDVH(any()) }
        }
    }

    @Nested
    inner class HentSakDvh {
        @Test
        fun `skal kreve forvalterrolle`() {
            // Act
            saksstatistikkController.hentSakDvh(1L)

            // Assert
            verify(exactly = 1) {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.FORVALTER,
                    handling = "Hente saksstatistikk for sak",
                )
            }
        }

        @Test
        fun `skal ikke hente saksstatistikk for sak når kaller ikke er forvalter`() {
            // Arrange
            every {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.FORVALTER,
                    handling = "Hente saksstatistikk for sak",
                )
            } throws RolleTilgangskontrollFeil("Krever FORVALTER")

            // Act & Assert
            val feilmelding =
                assertThrows<RolleTilgangskontrollFeil> {
                    saksstatistikkController.hentSakDvh(1L)
                }.message

            assertThat(feilmelding).isEqualTo("Krever FORVALTER")
            verify(exactly = 0) { saksstatistikkService.mapTilSakDvh(any()) }
        }
    }
}
