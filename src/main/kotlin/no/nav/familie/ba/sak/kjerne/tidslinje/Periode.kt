package no.nav.familie.ba.sak.kjerne.eøs.temaperiode

import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

val idGenerator = AtomicLong()

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
    val avhengerAv: Collection<Long> = emptyList()
) {
    val id = idGenerator.incrementAndGet()
}

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

fun LocalDate?.tilTidspunktEllerUendeligLengeSiden(default: LocalDate) =
    this?.let { Tidspunkt(this) } ?: Tidspunkt.uendeligLengeSiden(default)

fun LocalDate?.tilTidspunktEllerUendeligLengeTil(default: LocalDate) =
    this?.let { Tidspunkt(this) } ?: Tidspunkt.uendeligLengeSiden(default)

fun <U> PeriodeUtsnitt<*>.mapInnhold(nyttInhold: U) = PeriodeInnhold(nyttInhold, listOf(this.id))
