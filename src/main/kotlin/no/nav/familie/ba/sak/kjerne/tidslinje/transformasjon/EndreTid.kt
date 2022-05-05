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
 * Extension-metode for å konvertere fra Dag-tidslinje til Måned-tidslinje
 * Innholdet hentes fra innholdet siste dag i måneden
 */
fun <I> Tidslinje<I, Dag>.tilMånedFraSisteDagIMåneden(): Tidslinje<I, Måned> {

    val dagTidslinje = this

    return object : Tidslinje<I, Måned>() {
        val fraOgMed = dagTidslinje.fraOgMed().tilInneværendeMåned()
        val tilOgMed = dagTidslinje.tilOgMed().tilInneværendeMåned()

        override fun lagPerioder(): Collection<Periode<I, Måned>> {
            val månedTidsrom = fraOgMed..tilOgMed
            return månedTidsrom.map { måned ->
                val innholdSisteDag = dagTidslinje.innholdForTidspunkt(måned.tilSisteDagIMåneden())
                Periode(måned, måned, innholdSisteDag)
            }
        }
    }
}

/**
 * Extension-metode for å konvertere fra Måned-tidslinje til Dag-tidslinje
 * Første dag i fra-og-med-måneden brukes som første dag i perioden
 * Siste dag i til-og-med-måneden brukes som siste dag i perioden
 */
fun <I> Tidslinje<I, Måned>.tilDag(): Tidslinje<I, Dag> {

    val månedTidslinje = this

    return object : Tidslinje<I, Dag>() {
        override fun lagPerioder(): Collection<Periode<I, Dag>> =
            månedTidslinje.perioder().map {
                Periode(
                    it.fraOgMed.tilFørsteDagIMåneden(),
                    it.tilOgMed.tilSisteDagIMåneden(),
                    it.innhold
                )
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
