package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.map
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt

fun <T : Tidsenhet> String.somBolskTidslinje(t: Tidspunkt<T>) = this.tilCharTidslinje(t).somBolsk()

fun <T : Tidsenhet> Tidslinje<Char, T>.somBolsk() = this.map {
    when (it?.lowercaseChar()) {
        't' -> true
        'f' -> false
        else -> null
    }
}
