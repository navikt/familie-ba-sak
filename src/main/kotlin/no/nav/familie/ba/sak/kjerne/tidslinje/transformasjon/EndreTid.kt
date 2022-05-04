package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.innholdForTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.rangeTo
import no.nav.familie.ba.sak.kjerne.tidslinje.tilOgMed

/**
 * Extension-metode for å konvertere fra Dag-tidslinje til Måned-tidslinje
 * mapper-funksjonen tar inn listen av alle dagverdiene i én måned, og returner verdien måneden skal ha
 * Dagverdiene kommer i samme rekkefølge som dagene i måneden, og vil ha null-verdi hvis dagen ikke har en verdi
 */
fun <I, R> Tidslinje<I, Dag>.tilMåned(mapper: (List<I?>) -> R?): Tidslinje<R, Måned> {

    val dagTidslinje = this

    return object : Tidslinje<R, Måned>() {
        val fraOgMed = dagTidslinje.fraOgMed().tilInneværendeMåned()
        val tilOgMed = dagTidslinje.tilOgMed().tilInneværendeMåned()

        override fun lagPerioder(): Collection<Periode<R, Måned>> {
            val månedTidsrom = fraOgMed..tilOgMed
            return månedTidsrom.map { måned ->
                val dagerIMåned = måned.tilFørsteDagIMåneden()..måned.tilSisteDagIMåneden()
                val innholdAlleDager = dagerIMåned.map { dag -> dagTidslinje.innholdForTidspunkt(dag) }

                Periode(måned, måned, mapper(innholdAlleDager))
            }
        }
    }
}

/**
 * Extension-metode for å konvertere en tidslinje med uendelig fra-og-med og/eller til-og-med
 * til en endelig tidslinje basert på de underliggende tidspunktene
 * Tidslinjen
 * '<aaa bbbb   d>'
 * vil etter konvertering se slik ut
 * aaa bbbb   d
 */
fun <I, T : Tidsenhet> Tidslinje<I, T>.somEndelig(): Tidslinje<I, T> {
    val tidslinje = this
    return object : Tidslinje<I, T>() {
        override fun lagPerioder(): Collection<Periode<I, T>> =
            tidslinje.perioder().map {
                Periode(it.fraOgMed.somEndelig(), it.tilOgMed.somEndelig(), it.innhold)
            }
    }
}
