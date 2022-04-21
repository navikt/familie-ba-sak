package no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt

fun <I, T : Tidsenhet> Tidslinje<I, T>.filtrer(filter: (I?) -> Boolean): Tidslinje<I, T> {
    val tidslinje = this

    return object : Tidslinje<I, T>() {
        override fun fraOgMed(): Tidspunkt<T> =
            tidslinje.perioder().first { filter(it.innhold) }.fraOgMed

        override fun tilOgMed(): Tidspunkt<T> =
            tidslinje.perioder().last { filter(it.innhold) }.tilOgMed

        override fun lagPerioder(): Collection<Periode<I, T>> =
            tidslinje.perioder().filter { filter(it.innhold) }
    }
}

fun <I, T : Tidsenhet> Tidslinje<I, T>.filtrerIkkeNull(): Tidslinje<I, T> = filtrer { it != null }
