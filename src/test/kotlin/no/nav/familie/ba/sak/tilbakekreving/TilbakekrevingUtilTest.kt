package no.nav.familie.ba.sak.tilbakekreving

import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class TilbakekrevingUtilTest {

    @Test
    fun `test validerVerdierPåRestTilbakekreving kaster feil ved tilbakekreving uten feilutbetaling`() {

        assertThrows<Exception> {
            validerVerdierPåRestTilbakekreving(restTilbakekreving = RestTilbakekreving(
                    0,
                    Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                    begrunnelse = "",
            ), feilutbetaling = BigDecimal.ZERO)
        }
    }

    @Test
    fun `test validerVerdierPåRestTilbakekreving kaster feil ved ingen tilbakekreving når det er en feilutbetaling`() {

        assertThrows<Exception> {
            validerVerdierPåRestTilbakekreving(restTilbakekreving = null,
                                               feilutbetaling = BigDecimal.ONE)
        }
    }
}