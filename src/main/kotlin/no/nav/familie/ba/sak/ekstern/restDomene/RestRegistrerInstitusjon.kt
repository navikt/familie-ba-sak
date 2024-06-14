package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.institusjon.Institusjon

data class RestInstitusjon(
    val orgNummer: String?,
    val tssEksternId: String?,
    val navn: String? = null,
)

data class RestRegistrerInstitusjon(
    val orgNummer: String,
    val tssEksternId: String,
) {
    fun tilInstitusjon(): Institusjon =
        Institusjon(
            orgNummer = orgNummer,
            tssEksternId = tssEksternId,
        )
}
