package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tilOgMed

fun <I, T : Tidsenhet> Tidslinje<I, T>.beskjærEtter(tidslinje: Tidslinje<*, T>): Tidslinje<I, T> =
    beskjær(tidslinje.fraOgMed(), tidslinje.tilOgMed())

/**
 * Extension-metode for å beskjære (forkorte) en tidslinje
 * Etter beskjæringen vil tidslinjen strekke seg fra innsendt [fraOgMed] og til [tilOgMed]
 * Perioder som ligger helt utenfor grensene vil forsvinne.
 * Perioden i hver ende som ligger delvis innenfor, vil forkortes.
 * Hvis ny og eksisterende grenseverdi begge er uendelige, vil den nye benyttes
 */
fun <I, T : Tidsenhet> Tidslinje<I, T>.beskjær(fraOgMed: Tidspunkt<T>, tilOgMed: Tidspunkt<T>): Tidslinje<I, T> {

    val tidslinje = this

    if (fraOgMed < tidslinje.fraOgMed()) {
        throw IllegalArgumentException("fraOgMed kan ikke være tidligere enn starten på tidslinjen")
    }

    if (tilOgMed > tidslinje.tilOgMed()) {
        throw IllegalArgumentException("tilOgMed kan ikke være senere enn slutten på tidslinjen")
    }

    return if (tilOgMed < fraOgMed)
        TomTidslinje()
    else object : Tidslinje<I, T>() {
        override fun lagPerioder(): Collection<Periode<I, T>> {
            return tidslinje.perioder()
                .filter { it.fraOgMed <= tilOgMed && it.tilOgMed >= fraOgMed }
                .map {
                    when {
                        it.fraOgMed <= fraOgMed -> Periode(fraOgMed, it.tilOgMed, it.innhold)
                        it.tilOgMed >= tilOgMed -> Periode(it.fraOgMed, tilOgMed, it.innhold)
                        else -> it
                    }
                }
        }
    }
}
