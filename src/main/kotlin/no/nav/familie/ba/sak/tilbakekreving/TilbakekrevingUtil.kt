package no.nav.familie.ba.sak.tilbakekreving

import no.nav.familie.ba.sak.common.FunksjonellFeil
import java.math.BigDecimal

fun validerVerdierPåRestTilbakekreving(restTilbakekreving: RestTilbakekreving?, feilutbetaling: BigDecimal) {
    if (feilutbetaling != BigDecimal.ZERO && restTilbakekreving == null) {
        throw FunksjonellFeil("Simuleringen har en feilutbetaling, men restTilbakekreving var null",
                              frontendFeilmelding = "Du må velge en tilbakekrevingsstrategi siden det er en feilutbetaling.")
    }
    if (feilutbetaling == BigDecimal.ZERO && restTilbakekreving != null) {
        throw FunksjonellFeil(
                "Simuleringen har ikke en feilutbetaling, men restTilbakekreving var ikke null",
                frontendFeilmelding = "Du kan ikke opprette en tilbakekreving når det ikke er en feilutbetaling."
        )
    }
}