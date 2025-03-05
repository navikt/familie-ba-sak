package no.nav.familie.ba.sak.ekstern.restDomene

data class RestForenkletTilbakekrevingsvedtak(
    val id: Long,
    val behandlingId: Long,
    val fritekst: String,
    val samtykke: Boolean,
)

data class RestOppdaterForenkletTilbakekrevingsvedtakFritekst(
    val fritekst: String,
)

data class RestOppdaterForenkletTilbakekrevingsvedtakSamtykke(
    val samtykke: Boolean,
)
