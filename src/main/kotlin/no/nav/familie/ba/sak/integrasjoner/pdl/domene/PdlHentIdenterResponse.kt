package no.nav.familie.ba.sak.integrasjoner.pdl.domene

class PdlHentIdenterResponse(val pdlIdenter: PdlIdenter?)

data class PdlIdenter(val identer: List<IdentInformasjon>)

data class IdentInformasjon(
    val ident: String,
    val historisk: Boolean,
    val gruppe: String,
)

fun List<IdentInformasjon>.hentAktivFødselsnummer(): String {
    return this.singleOrNull { it.gruppe == "AKTORID" && it.historisk == false }?.ident
        ?: throw Error("Finner ikke aktørId i Pdl")
}

fun List<IdentInformasjon>.hentAktivAktørId(): String {
    return this.singleOrNull { it.gruppe == "AKTORID" && it.historisk == false }?.ident
        ?: throw Error("Finner ikke aktørId i Pdl")
}
