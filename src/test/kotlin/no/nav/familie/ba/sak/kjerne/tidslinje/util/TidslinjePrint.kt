package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import java.time.YearMonth

fun Iterable<Tidslinje<*, YearMonth>>.print() = this.forEach { it.print() }
fun Tidslinje<*, YearMonth>.print() {
    println("${this.fraOgMed()..this.tilOgMed()} ${this.javaClass.name}")
    this.perioder().forEach { println(it) }
}
