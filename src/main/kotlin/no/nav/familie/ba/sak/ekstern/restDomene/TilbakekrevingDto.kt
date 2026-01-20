package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg

class TilbakekrevingDto(
    val valg: Tilbakekrevingsvalg,
    val varsel: String? = null,
    val begrunnelse: String,
    val tilbakekrevingsbehandlingId: String? = null,
) {
    init {
        if (varsel == null && valg == Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL) {
            throw Feil("Varsel må settes dersom vi har valgt tilbakekreving med varsel.")
        }
    }
}
