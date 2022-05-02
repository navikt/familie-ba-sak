package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet

fun <T : Tidsenhet> Iterable<Tidslinje<*, T>>.print() = this.forEach { it.print() }
fun <T : Tidsenhet> Tidslinje<*, T>.print() {
    println("${this.fraOgMed()..this.tilOgMed()} ${this.javaClass.name}")
    this.perioder().forEach { println(it) }
}
