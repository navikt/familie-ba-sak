package no.nav.familie.ba.sak.tilbakekreving

import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg

class RestTilbakekreving(
        val vedtakId: Long,
        val valg: Tilbakekrevingsvalg,
        val varsel: String? = null,
        val begrunnelse: String,
        val tilbakekrevingsbehandlingId: String? = null,
)
