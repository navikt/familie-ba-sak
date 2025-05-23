package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder

/**
 * Returnerer en tidslinje med par av hvert etterfølgende element i tidslinjen.
 *
 * val aTilD = "abcd"
 * val bokstavTidslinje = aTilF.tilCharTidslinje(jan(2020))
 * val bokstavParTidslinje = bokstavTidslinje.zipMedNeste(ZipPadding.FØR)
 *
 * println(bokstavTidslinje) //
 *     2020-01 - 2020-01: a | 2020-02 - 2020-02: b | 2020-03 - 2020-03: c | 2020-04 - 2020-04: d
 *
 * println(bokstavParTidslinje) //
 *     2020-01 - 2020-01: (null, a) | 2020-02 - 2020-02: (a, b) | 2020-03 - 2020-03: (b, c) | 2020-04 - 2020-04: (c, d)
 */
enum class ZipPadding {
    FØR,
    ETTER,
    INGEN_PADDING,
}

fun <T> Tidslinje<T>.zipMedNeste(zipPadding: ZipPadding = ZipPadding.INGEN_PADDING): Tidslinje<Pair<T?, T?>> {
    val padding = listOf(Periode(null, null, null))

    return when (zipPadding) {
        ZipPadding.FØR -> padding + tilPerioder()
        ZipPadding.ETTER -> tilPerioder() + padding
        ZipPadding.INGEN_PADDING -> tilPerioder()
    }.zipWithNext { forrige, denne ->
        val verdi = forrige.verdi to denne.verdi
        val fom = if (zipPadding == ZipPadding.ETTER) forrige.fom else denne.fom
        val tom = if (zipPadding == ZipPadding.ETTER) forrige.tom else denne.tom
        Periode(verdi, fom, tom)
    }.tilTidslinje()
}
