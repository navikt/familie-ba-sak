package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje

fun Iterable<Tidslinje<*>>.print() = this.forEach { it.print() }
fun Tidslinje<*>.print() {
    println("${this.fraOgMed()..this.tilOgMed()} ${this.javaClass.name}")
    this.perioder().forEach { println(it) }
}
