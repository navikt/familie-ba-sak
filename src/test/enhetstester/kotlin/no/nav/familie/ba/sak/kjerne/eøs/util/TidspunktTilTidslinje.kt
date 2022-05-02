package no.nav.familie.ba.sak.kjerne.eøs.util

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.TidspunktClosedRange
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.rangeTo

fun <T : Tidsenhet, I> TidspunktClosedRange<T>.tilTidslinje(innhold: () -> I): Tidslinje<I, T> {
    val fom = this.start
    val tom = this.endInclusive
    return object : Tidslinje<I, T>() {
        override fun fraOgMed() = fom
        override fun tilOgMed() = tom
        override fun lagPerioder(): Collection<Periode<I, T>> {
            return listOf(Periode(fom, tom, innhold()))
        }
    }
}

fun <T : Tidsenhet, I> Tidspunkt<T>.tilTidslinje(innhold: () -> I): Tidslinje<I, T> =
    this.rangeTo(this).tilTidslinje(innhold)
