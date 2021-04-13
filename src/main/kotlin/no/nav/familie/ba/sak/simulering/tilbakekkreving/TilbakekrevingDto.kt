package no.nav.familie.ba.sak.simulering.tilbakekkreving

class TilbakekrevingDto (
    val vedtakId: Long,
    val type: TilbakekrevingType,
    val varsel: String?,
    val beskrivelse: String,
)
