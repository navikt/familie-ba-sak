package no.nav.familie.ba.sak.simulering.tilbakekreving

import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg

class TilbakekrevingDto (
        val vedtakId: Long,
        val type: Tilbakekrevingsvalg,
        val varsel: String? = null,
        val beskrivelse: String,
)
