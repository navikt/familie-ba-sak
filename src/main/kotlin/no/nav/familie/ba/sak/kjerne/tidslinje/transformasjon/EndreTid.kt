package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.innholdForTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.innholdsresultatForTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.tidslinjeFraTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.tilVerdi
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.forrige
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.somEndelig
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilForrigeMåned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilFørsteDagIMåneden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilInneværendeMåned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilNesteMåned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilSisteDagIMåneden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom.rangeTo
import no.nav.familie.ba.sak.kjerne.tidslinje.tilOgMed

/**
 * Extension-metode for å konvertere fra Dag-tidslinje til Måned-tidslinje
 * mapper-funksjonen tar inn listen av alle dagverdiene i én måned, og returner verdien måneden skal ha
 * Dagverdiene kommer i samme rekkefølge som dagene i måneden, og vil ha null-verdi hvis dagen ikke har en verdi
 */
fun <I, R> Tidslinje<I, Dag>.tilMåned(mapper: (List<I?>) -> R?): Tidslinje<R, Måned> {
    val dagTidslinje = this

    return object : Tidslinje<R, Måned>() {
        val fraOgMed = dagTidslinje.fraOgMed()?.tilInneværendeMåned()
        val tilOgMed = dagTidslinje.tilOgMed()?.tilInneværendeMåned()

        override fun lagPerioder(): Collection<Periode<R, Måned>> {
            return if (tilOgMed == null || fraOgMed == null) {
                emptyList()
            } else {
                (fraOgMed..tilOgMed).map { måned ->
                    val dagerIMåned = måned.tilFørsteDagIMåneden()..måned.tilSisteDagIMåneden()
                    val innholdAlleDager = dagerIMåned.map { dag -> dagTidslinje.innholdForTidspunkt(dag) }

                    Periode(måned, måned, mapper(innholdAlleDager))
                }
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
        val fraOgMed = dagTidslinje.fraOgMed()?.tilInneværendeMåned()
        val tilOgMed = dagTidslinje.tilOgMed()?.tilInneværendeMåned()

        override fun lagPerioder(): Collection<Periode<I, Måned>> {
            return if (tilOgMed == null || fraOgMed == null) {
                emptyList()
            } else {
                (fraOgMed..tilOgMed).map { måned ->
                    val innholdSisteDag = dagTidslinje.innholdForTidspunkt(måned.tilSisteDagIMåneden())
                    Periode(måned, måned, innholdSisteDag)
                }
            }
        }
    }
}

/**
 * Extention-metode som konverterer en dag-basert tidslinje til en måned-basert tidslinje.
 * <mapper>-funksjonen tar inn verdiene fra de to dagene før og etter månedsskiftet,
 * det vil si verdiene fra siste dag i forrige måned og første dag i inneværemde måned.
 * <mapper>-funksjonen kalles bare dersom begge dagene har en verdi.
 * Return-verdien er innholdet som blir brukt for inneværende måned.
 * Hvis retur-verdien er <null>, vil den resulterende måneden mangle verdi.
 * Funksjonen vil bruke månedsskiftene fra måneden før tidslinjens <fraOgMed> frem til og med måneden etter <tilOgMed>
 */
fun <I, R> Tidslinje<I, Dag>.tilMånedFraMånedsskifteIkkeNull(
    mapper: (innholdSisteDagForrigeMåned: I, innholdFørsteDagDenneMåned: I) -> R?
): Tidslinje<R, Måned> {
    val fraOgMed = fraOgMed()
    val tilOgMed = tilOgMed()

    return if (fraOgMed == null || tilOgMed == null) {
        TomTidslinje()
    } else {
        (fraOgMed.tilForrigeMåned()..tilOgMed.tilNesteMåned()).tidslinjeFraTidspunkt { måned ->
            val innholdSisteDagForrigeMåned = innholdsresultatForTidspunkt(måned.forrige().tilSisteDagIMåneden())
            val innholdFørsteDagDenneMåned = innholdsresultatForTidspunkt(måned.tilFørsteDagIMåneden())

            innholdSisteDagForrigeMåned
                .mapVerdi { s -> innholdFørsteDagDenneMåned.mapVerdi { mapper(s, it) } }.tilVerdi()
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
