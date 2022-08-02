package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.institusjon.Institusjon
import no.nav.familie.ba.sak.kjerne.verge.Verge
import java.math.BigInteger

data class EnsligMindreårligInfo(val Navn: String, val Adresse: String, val Ident: String?)

data class InstitusjonInfo(val orgNummer: String, val TSR: String)

data class RestRegistrerVerge(val ensligMindreårligInfo: EnsligMindreårligInfo?, val institusjonInfo: InstitusjonInfo?) {

    fun tilVerge(): Verge? = if (ensligMindreårligInfo != null) Verge(
        BigInteger.ZERO,
        ensligMindreårligInfo.Navn,
        ensligMindreårligInfo.Adresse,
        ensligMindreårligInfo.Ident
    ) else null

    fun tilInstitusjon(): Institusjon? = if (institusjonInfo != null) Institusjon(
        BigInteger.ZERO,
        institusjonInfo.orgNummer,
        institusjonInfo.TSR,
    ) else null
}
