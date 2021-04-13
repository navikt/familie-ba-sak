package no.nav.familie.ba.sak.simulering.tilbakekreving

class TilbakekrevingDto (
    val vedtakId: Long,
    val type: TilbakekrevingType,
    val varsel: String? = null,
    val beskrivelse: String,
)
