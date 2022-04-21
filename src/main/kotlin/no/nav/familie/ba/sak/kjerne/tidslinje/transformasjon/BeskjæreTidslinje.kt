package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt

fun <I, T : Tidsenhet> Tidslinje<I, T>.beskjærEtter(tidslinje: Tidslinje<*, T>): Tidslinje<I, T> =
    beskjær(tidslinje.fraOgMed(), tidslinje.tilOgMed())

fun <I, T : Tidsenhet> Tidslinje<I, T>.beskjær(fraOgMed: Tidspunkt<T>, tilOgMed: Tidspunkt<T>): Tidslinje<I, T> {

    val tidslinje = this

    return if (tilOgMed < fraOgMed)
        TomTidslinje()
    else object : Tidslinje<I, T>() {
        override fun fraOgMed() = fraOgMed
        override fun tilOgMed() = tilOgMed

        override fun lagPerioder(): Collection<Periode<I, T>> {
            return tidslinje.perioder()
                .filter { it.fraOgMed <= tilOgMed() && it.tilOgMed >= fraOgMed() }
                .map {
                    when {
                        it.fraOgMed == tidslinje.fraOgMed() -> Periode(fraOgMed(), it.tilOgMed, it.innhold)
                        it.fraOgMed < fraOgMed() -> Periode(fraOgMed(), it.tilOgMed, it.innhold)
                        it.tilOgMed == tidslinje.tilOgMed() -> Periode(it.fraOgMed, tilOgMed(), it.innhold)
                        it.tilOgMed > tilOgMed() -> Periode(it.fraOgMed, tilOgMed(), it.innhold)
                        else -> it
                    }
                }
        }
    }
}
