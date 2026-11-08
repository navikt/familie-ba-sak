package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SatsendringControllerTest {
    private val startSatsendring = mockk<StartSatsendring>(relaxed = true)
    private val tilgangService = mockk<TilgangService>()
    private val satsendringService = mockk<SatsendringService>(relaxed = true)
    private val satskjøringRepository = mockk<SatskjøringRepository>()

    private val satsendringController =
        SatsendringController(
            startSatsendring = startSatsendring,
            tilgangService = tilgangService,
            satsendringService = satsendringService,
            satskjøringRepository = satskjøringRepository,
        )

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.verifiserHarTilgangTilHandling(any(), any()) }
    }

    private fun verifiserForvalterrolleKrevd(handling: String) {
        verify(exactly = 1) {
            tilgangService.verifiserHarTilgangTilHandling(
                minimumBehandlerRolle = BehandlerRolle.FORVALTER,
                handling = handling,
            )
        }
    }

    private fun mockKallerErIkkeForvalter(handling: String) {
        every {
            tilgangService.verifiserHarTilgangTilHandling(
                minimumBehandlerRolle = BehandlerRolle.FORVALTER,
                handling = handling,
            )
        } throws RolleTilgangskontrollFeil("Krever FORVALTER")
    }

    @Nested
    inner class UtførSatsendringITaskPåFagsak {
        private val handling = "Trigge satsendring for fagsak"

        @Test
        fun `skal kreve forvalterrolle`() {
            // Act
            satsendringController.utførSatsendringITaskPåFagsak(1L)

            // Assert
            verifiserForvalterrolleKrevd(handling)
            verify(exactly = 1) { startSatsendring.opprettSatsendringForFagsak(1L) }
        }

        @Test
        fun `skal ikke trigge satsendring når kaller ikke er forvalter`() {
            // Arrange
            mockKallerErIkkeForvalter(handling)

            // Act & Assert
            val feilmelding =
                assertThrows<RolleTilgangskontrollFeil> {
                    satsendringController.utførSatsendringITaskPåFagsak(1L)
                }.message

            assertThat(feilmelding).isEqualTo("Krever FORVALTER")
            verify(exactly = 0) { startSatsendring.opprettSatsendringForFagsak(any()) }
        }
    }

    @Nested
    inner class UtførSatsendringITaskPåFagsaker {
        private val handling = "Trigge satsendring for fagsaker"

        @Test
        fun `skal kreve forvalterrolle`() {
            // Act
            satsendringController.utførSatsendringITaskPåFagsaker(setOf(1L, 2L))

            // Assert
            verifiserForvalterrolleKrevd(handling)
            verify(exactly = 2) { startSatsendring.opprettSatsendringForFagsak(any()) }
        }

        @Test
        fun `skal ikke trigge satsendring når kaller ikke er forvalter`() {
            // Arrange
            mockKallerErIkkeForvalter(handling)

            // Act & Assert
            val feilmelding =
                assertThrows<RolleTilgangskontrollFeil> {
                    satsendringController.utførSatsendringITaskPåFagsaker(setOf(1L, 2L))
                }.message

            assertThat(feilmelding).isEqualTo("Krever FORVALTER")
            verify(exactly = 0) { startSatsendring.opprettSatsendringForFagsak(any()) }
        }
    }

    @Nested
    inner class KanKjøreSatsendringPåFagsak {
        private val handling = "Sjekke om satsendring kan kjøres på fagsak"

        @Test
        fun `skal kreve forvalterrolle`() {
            // Arrange
            every { startSatsendring.kanGjennomføreSatsendringManuelt(1L) } returns true

            // Act
            val respons = satsendringController.kanKjøreSatsendringPåFagsak(1L)

            // Assert
            verifiserForvalterrolleKrevd(handling)
            assertThat(respons.body?.data).isTrue()
        }

        @Test
        fun `skal ikke sjekke satsendring når kaller ikke er forvalter`() {
            // Arrange
            mockKallerErIkkeForvalter(handling)

            // Act & Assert
            val feilmelding =
                assertThrows<RolleTilgangskontrollFeil> {
                    satsendringController.kanKjøreSatsendringPåFagsak(1L)
                }.message

            assertThat(feilmelding).isEqualTo("Krever FORVALTER")
            verify(exactly = 0) { startSatsendring.kanGjennomføreSatsendringManuelt(any()) }
        }
    }

    @Nested
    inner class UtførSatsendringPåListeIdenter {
        private val handling = "Trigge satsendring for liste med identer"

        @Test
        fun `skal kreve forvalterrolle`() {
            // Act
            satsendringController.utførSatsendringPåListeIdenter(setOf("12345678910"))

            // Assert
            verifiserForvalterrolleKrevd(handling)
            verify(exactly = 1) { startSatsendring.sjekkOgOpprettSatsendringVedGammelSats("12345678910") }
        }

        @Test
        fun `skal ikke trigge satsendring når kaller ikke er forvalter`() {
            // Arrange
            mockKallerErIkkeForvalter(handling)

            // Act & Assert
            val feilmelding =
                assertThrows<RolleTilgangskontrollFeil> {
                    satsendringController.utførSatsendringPåListeIdenter(setOf("12345678910"))
                }.message

            assertThat(feilmelding).isEqualTo("Krever FORVALTER")
            verify(exactly = 0) { startSatsendring.sjekkOgOpprettSatsendringVedGammelSats(any<String>()) }
        }
    }

    @Nested
    inner class FinnSakerUtenSisteSats {
        private val handling = "Finne løpende fagsaker uten siste sats"

        @Test
        fun `skal kreve forvalterrolle`() {
            // Act
            satsendringController.finnSakerUtenSisteSats()

            // Assert
            verifiserForvalterrolleKrevd(handling)
            verify(exactly = 1, timeout = 1000) { satsendringService.finnLøpendeFagsakerUtenSisteSats(any()) }
        }

        @Test
        fun `skal ikke starte søk når kaller ikke er forvalter`() {
            // Arrange
            mockKallerErIkkeForvalter(handling)

            // Act & Assert
            val feilmelding =
                assertThrows<RolleTilgangskontrollFeil> {
                    satsendringController.finnSakerUtenSisteSats()
                }.message

            assertThat(feilmelding).isEqualTo("Krever FORVALTER")
            verify(exactly = 0) { satsendringService.finnLøpendeFagsakerUtenSisteSats(any()) }
        }
    }
}
