package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.innholdForTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.rangeTo

fun <I, R> Tidslinje<I, Dag>.tilMåned(mapper: (List<I?>) -> R?): Tidslinje<R, Måned> {

    val dagTidslinje = this

    return object : Tidslinje<R, Måned>() {
        override fun fraOgMed() = dagTidslinje.fraOgMed().tilInneværendeMåned()
        override fun tilOgMed() = dagTidslinje.tilOgMed().tilInneværendeMåned()

        override fun lagPerioder(): Collection<Periode<R, Måned>> {
            val månedTidsrom = fraOgMed()..tilOgMed()
            return månedTidsrom.map { måned ->
                val førsteDagIMåneden = måned.tilFørsteDagIMåneden()
                val sisteDagIMåneden = måned.tilSisteDagIMåneden()

                val dagerIMåned = førsteDagIMåneden..sisteDagIMåneden
                val dagsinnhold = dagerIMåned.map { dag -> dagTidslinje.innholdForTidspunkt(dag) }

                val månedInnhold = mapper(dagsinnhold)

                Periode(måned, måned, månedInnhold)
            }
        }
    }
}
