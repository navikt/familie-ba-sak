package no.nav.familie.ba.sak.brev.domene.maler

interface Brev {
    fun toFamilieBrevString(): String
    val delmalData: Any
    val flettefelter: Any
}

typealias Flettefelt = List<String>

fun flettefelt(flettefeltData: String): Flettefelt = listOf(flettefeltData)
fun flettefelt(flettefeltData: List<String>): Flettefelt = flettefeltData

fun no.nav.familie.ba.sak.dokument.domene.BrevType.tilNyBrevType()  = when(this.malId) {
    "innhente-opplysninger" -> BrevType.INNHENTE_OPPLYSNINGER
    else -> error("Kan ikke mappe brevmal ${this.visningsTekst} til ny brevtype da denne ikke er støttet i ny løsning enda.")
}

enum class BrevType(val apiNavn: String, val visningsTekst: String) {
    INNHENTE_OPPLYSNINGER("innhenteOpplysninger", "Innhente opplysninger");
}

enum class DelmalType(val apiNavn: String, val visningsTekst: String) {
    SIGNATUR("signatur", "Signatur");
}