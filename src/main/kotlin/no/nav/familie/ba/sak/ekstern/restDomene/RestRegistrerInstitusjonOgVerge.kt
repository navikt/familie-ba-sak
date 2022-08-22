package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.institusjon.Institusjon
import no.nav.familie.ba.sak.kjerne.verge.Verge

data class VergeInfo(val navn: String?, val adresse: String?, val ident: String)

data class InstitusjonInfo(val orgNummer: String, val eksternTssNummer: String)

data class RestRegistrerInstitusjonOgVerge(
    val vergeInfo: VergeInfo?,
    val institusjonInfo: InstitusjonInfo?
) {

    fun tilVerge(behandling: Behandling): Verge? = if (vergeInfo != null) {
        Verge(
        ident = vergeInfo.ident,
        behandling = behandling
    )
    } else {
        null
    }

    fun tilInstitusjon(): Institusjon? = if (institusjonInfo != null) {
        Institusjon(
        orgNummer = institusjonInfo.orgNummer,
        tssEksternId = institusjonInfo.eksternTssNummer,
    )
    } else {
        null
    }
}
