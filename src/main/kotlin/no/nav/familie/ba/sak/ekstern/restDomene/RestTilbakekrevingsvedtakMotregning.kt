package no.nav.familie.ba.sak.ekstern.restDomene

data class RestTilbakekrevingsvedtakMotregning(
    val id: Long,
    val behandlingId: Long,
    val fritekst: String,
    val samtykke: Boolean,
)

data class RestOppdaterTilbakekrevingsvedtakMotregningFritekst(
    val fritekst: String,
)

data class RestOppdaterTilbakekrevingsvedtakMotregningSamtykke(
    val samtykke: Boolean,
)
