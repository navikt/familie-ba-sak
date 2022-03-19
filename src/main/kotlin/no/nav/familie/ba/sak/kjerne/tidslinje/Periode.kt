package no.nav.familie.ba.sak.kjerne.eøs.temaperiode

import java.time.LocalDate
import java.time.YearMonth

fun <T, U> Iterable<Periode<T>>.mapInnhold(mapper: (T?) -> U?) =
    this.map { Periode<U>(it.fom, it.tom, mapper(it.innhold)) }

data class Periode<T>(
    val fom: Tidspunkt,
    val tom: Tidspunkt,
    val innhold: T? = null,
) {
    constructor(tidsrom: Tidsrom, innhold: T?) : this(tidsrom.start, tidsrom.endInclusive, innhold)

    override fun toString(): String = "$fom - $tom: $innhold"
}

fun LocalDate?.tilTidspunktEllerUendeligLengeSiden(default: () -> LocalDate) =
    this?.let { DagTidspunkt(this) } ?: Tidspunkt.uendeligLengeSiden(default())

fun LocalDate?.tilTidspunktEllerUendeligLengeTil(default: () -> LocalDate) =
    this?.let { DagTidspunkt(this) } ?: Tidspunkt.uendeligLengeTil(default())

fun YearMonth?.tilTidspunktEllerUendeligLengeSiden(default: () -> YearMonth) =
    this?.let { MånedTidspunkt(this) } ?: Tidspunkt.uendeligLengeSiden(default())

fun YearMonth?.tilTidspunktEllerUendeligLengeTil(default: () -> YearMonth) =
    this?.let { MånedTidspunkt(this) } ?: Tidspunkt.uendeligLengeTil(default())

data class PeriodeSplitt<T>(
    val tidspunkt: Tidspunkt,
    val før: Periode<T>,
    val etter: Periode<T>
) {
    constructor(tidspunkt: Tidspunkt) :
        this(tidspunkt, Periode(tidspunkt, tidspunkt), Periode(tidspunkt, tidspunkt))
}

fun <T> PeriodeSplitt<*>.påførSplitt(
    periode: Periode<T>,
    etterfølgendeTidslinjer: Iterable<TidslinjeMedAvhengigheter<*>>
): Iterable<Periode<T>> {
    if (periode.tom <= this.tidspunkt) {
        return listOf(periode)
    }

    if (periode.fom >= this.tidspunkt) {
        return listOf(periode)
    }

    val førSplitt = periode.copy(tom = tidspunkt)
    val etterSplitt = periode.copy(fom = tidspunkt.neste())
    val splitt = PeriodeSplitt(tidspunkt, førSplitt, etterSplitt)

    etterfølgendeTidslinjer.forEach { it.splitt(splitt) }

    return listOf(førSplitt, etterSplitt)
}
