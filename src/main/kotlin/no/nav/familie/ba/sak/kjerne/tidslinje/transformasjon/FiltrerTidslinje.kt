package no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærEtter

/**
 * Extension-metode for å filtrere tidslinjen mot et filter
 * Resultatet kan bli en kortere tidslinje, og en TomTidslinje hvis ingen perioder passerer filteret
 * Merk at fraOgMed() og tilOgMed() på tidslinjen vil føre til at periodene genereres for underliggende tidslinje
 */
fun <I, T : Tidsenhet> Tidslinje<I, T>.filtrer(filter: (I?) -> Boolean): Tidslinje<I, T> {
    val tidslinje = this
    val fraOgMed = tidslinje.perioder().firstOrNull { filter(it.innhold) }?.fraOgMed
    val tilOgMed = tidslinje.perioder().lastOrNull { filter(it.innhold) }?.tilOgMed

    // fraOgMed og tilOgMed vil enten begge ha verdi eller begge være null
    // Sjekker begge for å få smart cast for begge i metodekallene under
    return if (fraOgMed == null || tilOgMed == null)
        TomTidslinje()
    else object : Tidslinje<I, T>() {
        override fun lagPerioder(): Collection<Periode<I, T>> =
            tidslinje.perioder().filter { filter(it.innhold) }
    }
}

fun <I, T : Tidsenhet> Tidslinje<I, T>.filtrerIkkeNull(): Tidslinje<I, T> = filtrer { it != null }

/**
 * Extension-metode for å filtrere tidslinjen mot en boolsk tidslinje
 * Resultatet får samme lengde som tidslinjen det opereres på
 * Det vil finnes perioder som tilsvarer periodene fra kilde-tidslinjen,
 * men innholdet blir null hvis den boolske tidslinjen er false
 */
fun <I, T : Tidsenhet> Tidslinje<I, T>.filtrerMed(boolskTidslinje: Tidslinje<Boolean, T>): Tidslinje<I, T> {
    return this.kombinerMed(boolskTidslinje) { innhold, erSann ->
        when (erSann) {
            true -> innhold
            else -> null
        }
    }.beskjærEtter(this)
}
