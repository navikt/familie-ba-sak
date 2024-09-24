package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import no.nav.person.pdl.aktor.v2.Type

class PdlHentIdenterResponse(
    val pdlIdenter: PdlIdenter?,
)

data class PdlIdenter(
    val identer: List<IdentInformasjon>,
)

data class IdentInformasjon(
    val ident: String,
    val historisk: Boolean,
    val gruppe: String,
)

fun List<IdentInformasjon>.hentAktivFødselsnummer(): String =
    this.singleOrNull { it.gruppe == Type.FOLKEREGISTERIDENT.name && !it.historisk }?.ident
        ?: throw Error("Finner ikke aktørId i Pdl")

fun List<IdentInformasjon>.hentAktivAktørId(): String =
    this.singleOrNull { it.gruppe == Type.AKTORID.name && !it.historisk }?.ident
        ?: throw Error("Finner ikke aktørId i Pdl")

fun List<IdentInformasjon>.hentAktørIder(): List<String> = filter { it.gruppe == Type.AKTORID.name }.map { it.ident }
