package no.nav.familie.ba.sak.kjerne.eøs.temaperiode

import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import java.time.LocalDate

data class PeriodeUtsnitt<T>(
    val innhold: T? = null,
    val id: Long
)

data class PeriodeInnhold<T>(
    val innhold: T? = null,
    val avhengerAv: Collection<Long>
)

fun <T> PeriodeUtsnitt<T>.inneholder(innhold: T) = this.innhold == innhold
fun <T> PeriodeInnhold<T>.tilPeriode(tidspunkt: Tidspunkt): Periode<T> {
    return Periode(tidspunkt, tidspunkt, this.innhold, this.avhengerAv)
}

data class Periode<T>(
    val fom: Tidspunkt,
    val tom: Tidspunkt,
    val innhold: T? = null,
    val avhengerAv: Collection<Long> = emptyList(),
    val id: Long = 0
)

fun <T> Periode<T>.tilUtsnitt() =
    PeriodeUtsnitt(this.innhold, this.id)

fun <T> Periode<T>.tilInnhold() =
    PeriodeInnhold(this.innhold, avhengerAv)

data class ForeldetUtbetaling(
    val begrunnelse: String
)

data class AndelTilkjentYtelse(
    val kalkulerBeløp: Double,
    val ytelseType: YtelseType
)

data class PeriodeSplitt<T>(
    val tidspunkt: Tidspunkt,
    val originalId: Long,
    val før: Periode<T>,
    val etter: Periode<T>
) {
    constructor(tidspunkt: Tidspunkt) :
        this(tidspunkt, 0, Periode(tidspunkt, tidspunkt), Periode(tidspunkt, tidspunkt))
}

fun LocalDate?.tilTidspunktEllerUendeligLengeSiden(default: () -> LocalDate) =
    this?.let { Tidspunkt(this) } ?: Tidspunkt.uendeligLengeSiden(default())

fun LocalDate?.tilTidspunktEllerUendeligLengeTil(default: () -> LocalDate) =
    this?.let { Tidspunkt(this) } ?: Tidspunkt.uendeligLengeSiden(default())

fun <U> PeriodeUtsnitt<*>.mapInnhold(nyttInhold: U) = PeriodeInnhold(nyttInhold, listOf(this.id))

fun <T> Periode<T>.erstattAvhengighet(originalId: Long, nyId: Long) =
    if (this.avhengerAv.contains(originalId)) {
        this.copy(avhengerAv = this.avhengerAv.filter { it != originalId } + nyId)
    } else
        this

fun <T> PeriodeSplitt<*>.påførSplitt(
    periode: Periode<T>,
    etterfølgendeTidslinjer: Iterable<TidslinjeMedAvhengigheter<*>>
): Iterable<Periode<T>> {
    if (periode.tom <= this.tidspunkt) {
        return listOf(periode.erstattAvhengighet(originalId, før.id))
    }

    if (periode.fom >= this.tidspunkt) {
        return listOf(periode.erstattAvhengighet(originalId, etter.id))
    }

    val førSplitt = periode.copy(tom = tidspunkt).erstattAvhengighet(originalId, før.id)
    val etterSplitt = periode.copy(fom = tidspunkt.neste()).erstattAvhengighet(originalId, etter.id)
    val splitt = PeriodeSplitt(tidspunkt, periode.id, førSplitt, etterSplitt)

    etterfølgendeTidslinjer.forEach { it.splitt(splitt) }

    return listOf(førSplitt, etterSplitt)
}
