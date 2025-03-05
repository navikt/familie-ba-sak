package no.nav.familie.ba.sak.ekstern.restDomene

data class RestForenkletTilbakekrevingVedtak(
    val id: Long,
    val behandlingId: Long,
    val fritekst: String,
    val samtykke: Boolean,
)

data class RestOppdaterForenkletTilbakekrevingVedtakFritekst(
    val fritekst: String,
)

data class RestOppdaterForenkletTilbakekrevingVedtakSamtykke(
    val samtykke: Boolean,
)
