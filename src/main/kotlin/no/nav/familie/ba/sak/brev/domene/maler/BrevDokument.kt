package no.nav.familie.ba.sak.brev.domene.maler

interface Brev {
    fun toFamilieBrevString(): String
    val delmalData: Any
    val flettefelter: Any
}

typealias Flettefelt = List<String>

fun flettefelt(flettefeltData: String): Flettefelt = listOf(flettefeltData)
fun flettefelt(flettefeltData: List<String>): Flettefelt = flettefeltData