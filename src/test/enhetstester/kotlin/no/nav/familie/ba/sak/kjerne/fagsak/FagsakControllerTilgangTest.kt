package no.nav.familie.ba.sak.kjerne.fagsak

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class FagsakControllerTilgangTest {
    private val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    private val tilgangService = mockk<TilgangService>()

    private val fagsakController =
        FagsakController(
            fagsakService = mockk(),
            personidentService = mockk(),
            tilgangService = tilgangService,
            tilbakekrevingService = mockk(),
            vedtaksperiodeService = vedtaksperiodeService,
        )

    private val fagsakId = 1L

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilHandlingOgFagsak(any(), any(), any(), any()) }
        every { vedtaksperiodeService.hentGjeldendeUtbetalingsperioder(fagsakId) } returns emptyList()
    }

    @Nested
    inner class HentGjeldendeUtbetalingsperioder {
        @Test
        fun `skal validere tilgang til hele fagsaken og kreve veilederrolle`() {
            // Act
            fagsakController.hentGjeldendeUtbetalingsperioder(fagsakId)

            // Assert
            verify(exactly = 1) {
                tilgangService.validerTilgangTilHandlingOgFagsak(
                    fagsakId = fagsakId,
                    event = AuditLoggerEvent.ACCESS,
                    minimumBehandlerRolle = BehandlerRolle.VEILEDER,
                    handling = "hente gjeldende utbetalingsperioder",
                )
            }
        }

        @Test
        fun `skal returnere utbetalingsperiodene fra vedtaksperiodeService`() {
            // Arrange
            val utbetalingsperiode =
                Utbetalingsperiode(
                    periodeFom = LocalDate.of(2026, 1, 1),
                    periodeTom = LocalDate.of(2026, 12, 31),
                    vedtaksperiodetype = Vedtaksperiodetype.UTBETALING,
                    utbetalingsperiodeDetaljer = emptyList(),
                    ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD),
                    antallBarn = 1,
                    utbetaltPerMnd = 1968,
                )
            every { vedtaksperiodeService.hentGjeldendeUtbetalingsperioder(fagsakId) } returns listOf(utbetalingsperiode)

            // Act
            val respons = fagsakController.hentGjeldendeUtbetalingsperioder(fagsakId)

            // Assert
            assertThat(respons.body?.data).containsExactly(utbetalingsperiode)
        }

        @Test
        fun `skal ikke hente utbetalingsperioder når saksbehandler mangler tilgang til personene i fagsaken`() {
            // Arrange
            every {
                tilgangService.validerTilgangTilHandlingOgFagsak(
                    fagsakId = fagsakId,
                    event = AuditLoggerEvent.ACCESS,
                    minimumBehandlerRolle = BehandlerRolle.VEILEDER,
                    handling = "hente gjeldende utbetalingsperioder",
                )
            } throws RolleTilgangskontrollFeil("Mangler tilgang til fagsak")

            // Act & Assert
            val feilmelding =
                assertThrows<RolleTilgangskontrollFeil> {
                    fagsakController.hentGjeldendeUtbetalingsperioder(fagsakId)
                }.message

            assertThat(feilmelding).isEqualTo("Mangler tilgang til fagsak")
            verify(exactly = 0) { vedtaksperiodeService.hentGjeldendeUtbetalingsperioder(any()) }
        }
    }
}
