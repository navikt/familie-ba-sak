package no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet

fun <I, T : Tidsenhet, R> Tidslinje<I, T>.windowed(
    size: Int,
    step: Int = 1,
    partialWindows: Boolean = false,
    mapper: (List<Periode<I, T>>) -> Periode<R, T>
): Tidslinje<R, T> {

    val tidslinje = this

    return object : Tidslinje<R, T>() {
        override fun fraOgMed() = tidslinje.fraOgMed()
        override fun tilOgMed() = tidslinje.tilOgMed()

        override fun lagPerioder(): Collection<Periode<R, T>> =
            tidslinje.perioder().windowed(size, step, partialWindows) { perioder ->
                mapper(perioder)
            }
    }
}
