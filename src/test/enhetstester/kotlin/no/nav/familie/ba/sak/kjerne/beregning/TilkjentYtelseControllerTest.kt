package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

class TilkjentYtelseControllerTest {
    private val tilgangService = mockk<TilgangService>()
    private val barnetrygdHistorikkService = mockk<BarnetrygdHistorikkService>()
    private val controller = TilkjentYtelseController(tilgangService, barnetrygdHistorikkService)
    private val handling = "svarer om det har vært utbetaling til aktør på fagsak"

    @Test
    fun `skal returnere resultat fra service i ressurs for fagsak med tilgang`() {
        // Arrange
        val fagsakId = 1L
        justRun { tilgangService.verifiserHarTilgangTilHandling(BehandlerRolle.SYSTEM, handling) }
        every { barnetrygdHistorikkService.harSøkerHattInnvilgetBarnetrygd(fagsakId) } returns true

        // Act
        val response = controller.harSøkerHattUtbetaling(fagsakId)

        // Assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(response.body?.data).isTrue()
        verify(exactly = 1) { tilgangService.verifiserHarTilgangTilHandling(BehandlerRolle.SYSTEM, handling) }
        verify(exactly = 1) { barnetrygdHistorikkService.harSøkerHattInnvilgetBarnetrygd(fagsakId) }
    }

    @Test
    fun `skal returnere false når søker ikke har hatt utbetaling`() {
        // Arrange
        val fagsakId = 1L
        justRun { tilgangService.verifiserHarTilgangTilHandling(BehandlerRolle.SYSTEM, handling) }
        every { barnetrygdHistorikkService.harSøkerHattInnvilgetBarnetrygd(fagsakId) } returns false

        // Act
        val response = controller.harSøkerHattUtbetaling(fagsakId)

        // Assert
        assertThat(response.body?.data).isFalse()
    }

    @Test
    fun `skal ikke returnere utbetaling ved manglende systemtilgang`() {
        // Arrange
        val fagsakId = 1L
        every { tilgangService.verifiserHarTilgangTilHandling(BehandlerRolle.SYSTEM, handling) } throws RolleTilgangskontrollFeil("Ikke tilgang")

        // Act & Assert
        assertThrows<RolleTilgangskontrollFeil> { controller.harSøkerHattUtbetaling(fagsakId) }
        verify(exactly = 0) { barnetrygdHistorikkService.harSøkerHattInnvilgetBarnetrygd(any()) }
    }
}
