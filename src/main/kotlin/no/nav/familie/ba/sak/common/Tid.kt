package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

val TIDENES_MORGEN = LocalDate.MIN
val TIDENES_ENDE = LocalDate.MAX.minusDays(1)


fun LocalDate.tilKortString() = this.format(DateTimeFormatter.ofPattern("dd.MM.YY", nbLocale))
fun LocalDate.tilDagMånedÅr() = this.format(DateTimeFormatter.ofPattern("d. MMMM YYYY", nbLocale))
fun LocalDate.tilMånedÅr() = this.format(DateTimeFormatter.ofPattern("MMMM YYYY", nbLocale))

fun LocalDate.sisteDagIForrigeMåned(): LocalDate {
    val sammeDagForrigeMåned = this.minusMonths(1)
    return sammeDagForrigeMåned.sisteDagIMåned()
}

fun LocalDate.sisteDagIMåned(): LocalDate {
    return YearMonth.from(this).atEndOfMonth()
}

fun LocalDate.førsteDagINesteMåned() = this.plusMonths(1).withDayOfMonth(1)
fun LocalDate.førsteDagIInneværendeMåned() = this.withDayOfMonth(1)

fun LocalDate.isSameOrBefore(toCompare: LocalDate): Boolean {
    return this.isBefore(toCompare) || this == toCompare
}

fun LocalDate.isSameOrAfter(toCompare: LocalDate): Boolean {
    return this.isAfter(toCompare) || this == toCompare
}

private fun LocalDate.toDate(): Date = Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())

private fun LocalDate.isBetween(toCompare: Periode): Boolean {
    return this.isAfter(toCompare.fom) && this.isBefore(toCompare.tom)
}

fun Periode.kanErstatte(other: Periode): Boolean {
    return this.fom.isSameOrBefore(other.fom) && this.tom.isSameOrAfter(other.tom)
}

fun Periode.kanSplitte(other: Periode): Boolean {
    return this.fom.isBetween(other) && this.tom.isBetween(other)
}

fun Periode.kanFlytteFom(other: Periode): Boolean {
    return this.fom.isSameOrBefore(other.fom) && this.tom.isBetween(other)
}

fun Periode.kanFlytteTom(other: Periode): Boolean {
    return this.fom.isBetween(other) && this.tom.isSameOrAfter(other.tom)
}

data class Periode(val fom: LocalDate, val tom: LocalDate)

fun VilkårResultat.toPeriode(): Periode {
    return Periode(fom = this.periodeFom ?: throw Feil("Perioden har ikke fom-dato"),
            tom = this.periodeTom ?: TIDENES_ENDE)
}

fun VilkårResultat.erEtterfølgendePeriode(other: VilkårResultat): Boolean {
    return this.toPeriode().tom.month == other.toPeriode().fom.month &&
            this.toPeriode().tom.year == other.toPeriode().fom.year
}

fun RestVilkårResultat.toPeriode(): Periode {
    return Periode(fom = this.periodeFom ?: throw Feil("Perioden har ikke fom-dato"),
            tom = this.periodeTom ?: TIDENES_ENDE)
}


