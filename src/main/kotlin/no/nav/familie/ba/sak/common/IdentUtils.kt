package no.nav.familie.ba.sak.common

fun erDnummer(personIdent: String): Boolean = personIdent.substring(0, 1).toInt() > 3

fun erFDatnummer(personIdent: String): Boolean = personIdent.substring(6).toInt() == 0

/**
 * BOST-nr har måned mellom 21 og 32
 */
fun erBostNummer(personIdent: String): Boolean {
    personIdent.substring(2, 4).toInt().also { måned ->
        return måned in 21..32
    }
}
