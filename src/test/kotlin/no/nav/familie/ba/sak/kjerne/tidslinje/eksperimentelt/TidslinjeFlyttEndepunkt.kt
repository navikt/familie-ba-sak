package no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt

fun <I, T : Tidsenhet> Tidslinje<I, T>.flyttFraOgMed(tidsenheter: Long): Tidslinje<I, T> {

    val tidslinje = this
    val nyFraOgMed = tidslinje.fraOgMed().flytt(tidsenheter)

    if (nyFraOgMed > tidslinje.tilOgMed()) {
        return TomTidslinje()
    }

    return object : Tidslinje<I, T>() {
        override fun fraOgMed(): Tidspunkt<T> = nyFraOgMed
        override fun tilOgMed(): Tidspunkt<T> = tidslinje.tilOgMed()

        override fun lagPerioder(): Collection<Periode<I, T>> = tidslinje.perioder()
            .filter { it.tilOgMed >= fraOgMed() }
            .map {
                when {
                    it.fraOgMed == tidslinje.fraOgMed() -> Periode(fraOgMed(), it.tilOgMed, it.innhold)
                    it.fraOgMed < fraOgMed() -> Periode(fraOgMed(), it.tilOgMed, it.innhold)
                    else -> it
                }
            }
    }
}

fun <I, T : Tidsenhet> Tidslinje<I, T>.flyttTilOgMed(tidsenheter: Long): Tidslinje<I, T> {

    val tidslinje = this
    val nyTilOgMed = tidslinje.tilOgMed().flytt(tidsenheter)

    if (nyTilOgMed < tidslinje.fraOgMed()) {
        return TomTidslinje()
    }

    return object : Tidslinje<I, T>() {
        override fun fraOgMed(): Tidspunkt<T> = tidslinje.fraOgMed()
        override fun tilOgMed(): Tidspunkt<T> = nyTilOgMed

        override fun lagPerioder(): Collection<Periode<I, T>> = tidslinje.perioder()
            .filter { it.fraOgMed <= tilOgMed() }
            .map {
                when {
                    it.tilOgMed == tidslinje.tilOgMed() -> Periode(it.fraOgMed, tilOgMed(), it.innhold)
                    it.tilOgMed > tilOgMed() -> Periode(it.fraOgMed, tilOgMed(), it.innhold)
                    else -> it
                }
            }
    }
}
