package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.månedPeriodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.periodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import java.time.YearMonth

/**
 * val aTilF = ('a'..'f').toList().joinToString("")
 * val bokstavTidslinje = aTilF.tilCharTidslinje(YearMonth.now())
 * val bokstavParTidslinje = bokstavTidslinje.zipMedNeste()
 *
 * println(aTilF) //
 *     "abcdef"
 *
 * println(bokstavParTidslinje.perioder().map { it.innhold }) //
 *     [(null, a), (a, b), (b, c), (c, d), (d, e), (e, f)]
 */
fun <T> Tidslinje<T, Måned>.zipMedNeste(): Tidslinje<Pair<T?, T?>, Måned> = (
    listOf(
        månedPeriodeAv(YearMonth.now(), YearMonth.now(), null),
    ) + perioder()
    ).zipWithNext { forrige, denne ->
    periodeAv(denne.fraOgMed, denne.tilOgMed, Pair(forrige.innhold, denne.innhold))
}.tilTidslinje()
