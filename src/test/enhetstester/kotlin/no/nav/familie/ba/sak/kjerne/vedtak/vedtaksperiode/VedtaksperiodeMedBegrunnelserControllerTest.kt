package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VedtaksperiodeMedBegrunnelserControllerTest {
    private val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    private val tilgangService = mockk<TilgangService>()

    private val vedtaksperiodeMedBegrunnelserController =
        VedtaksperiodeMedBegrunnelserController(
            vedtaksperiodeService = vedtaksperiodeService,
            tilgangService = tilgangService,
            brevKlient = mockk(),
            utvidetBehandlingService = mockk(),
            vedtaksperiodeHentOgPersisterService = mockk(),
            kodeverkService = mockk(),
            genererTabulærTestService = mockk(),
        )

    private val behandlingId = 1L

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilBehandling(any(), any()) }
        justRun { tilgangService.verifiserHarTilgangTilHandling(any(), any()) }
        every { vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelserDto(behandlingId) } returns emptyList()
    }

    @Nested
    inner class HentUtvidetVedtaksperiodeMedBegrunnelserDto {
        @Test
        fun `skal validere tilgang til behandlingen og kreve veilederrolle`() {
            // Act
            vedtaksperiodeMedBegrunnelserController.hentUtvidetVedtaksperiodeMedBegrunnelserDto(behandlingId)

            // Assert
            verify(exactly = 1) {
                tilgangService.validerTilgangTilBehandling(
                    behandlingId = behandlingId,
                    event = AuditLoggerEvent.ACCESS,
                )
            }
            verify(exactly = 1) {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.VEILEDER,
                    handling = "hente vedtaksperioder",
                )
            }
        }

        @Test
        fun `skal ikke hente vedtaksperioder når saksbehandler mangler tilgang til personene i behandlingen`() {
            // Arrange
            every {
                tilgangService.validerTilgangTilBehandling(
                    behandlingId = behandlingId,
                    event = AuditLoggerEvent.ACCESS,
                )
            } throws RolleTilgangskontrollFeil("Mangler tilgang til behandling")

            // Act & Assert
            val feilmelding =
                assertThrows<RolleTilgangskontrollFeil> {
                    vedtaksperiodeMedBegrunnelserController.hentUtvidetVedtaksperiodeMedBegrunnelserDto(behandlingId)
                }.message

            assertThat(feilmelding).isEqualTo("Mangler tilgang til behandling")
            verify(exactly = 0) { vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelserDto(any()) }
        }

        @Test
        fun `skal ikke hente vedtaksperioder når saksbehandler mangler rolle`() {
            // Arrange
            every {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle = BehandlerRolle.VEILEDER,
                    handling = "hente vedtaksperioder",
                )
            } throws RolleTilgangskontrollFeil("Krever VEILEDER")

            // Act & Assert
            val feilmelding =
                assertThrows<RolleTilgangskontrollFeil> {
                    vedtaksperiodeMedBegrunnelserController.hentUtvidetVedtaksperiodeMedBegrunnelserDto(behandlingId)
                }.message

            assertThat(feilmelding).isEqualTo("Krever VEILEDER")
            verify(exactly = 0) { vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelserDto(any()) }
        }
    }
}
