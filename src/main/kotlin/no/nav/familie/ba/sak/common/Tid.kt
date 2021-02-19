package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

val TIDENES_MORGEN = LocalDate.MIN
val TIDENES_ENDE = LocalDate.MAX


fun LocalDate.tilddMMyy() = this.format(DateTimeFormatter.ofPattern("ddMMyy", nbLocale))
fun LocalDate.tilKortString() = this.format(DateTimeFormatter.ofPattern("dd.MM.yy", nbLocale))
fun YearMonth.tilKortString() = this.format(DateTimeFormatter.ofPattern("MM.yy", nbLocale))
fun LocalDate.tilDagMånedÅr() = this.format(DateTimeFormatter.ofPattern("d. MMMM yyyy", nbLocale))
fun LocalDate.tilMånedÅr() = this.format(DateTimeFormatter.ofPattern("MMMM yyyy", nbLocale))
fun YearMonth.tilMånedÅr() = this.format(DateTimeFormatter.ofPattern("MMMM yyyy", nbLocale))

fun LocalDate.sisteDagIForrigeMåned(): LocalDate {
    val sammeDagForrigeMåned = this.minusMonths(1)
    return sammeDagForrigeMåned.sisteDagIMåned()
}

fun LocalDate.sisteDagINesteMåned(): LocalDate {
    val sammeDagNesteMåned = this.plusMonths(1)
    return sammeDagNesteMåned.sisteDagIMåned()
}

fun LocalDate.toYearMonth() = YearMonth.from(this)
fun YearMonth.toLocalDate() = LocalDate.of(this.year, this.month, 1)

fun YearMonth.førsteDagIInneværendeMåned() = this.atDay(1)
fun YearMonth.sisteDagIInneværendeMåned() = this.atEndOfMonth()

fun LocalDate.forrigeMåned(): YearMonth {
    return this.toYearMonth().minusMonths(1)
}

fun YearMonth.forrigeMåned(): YearMonth {
    return this.minusMonths(1)
}

fun LocalDate.nesteMåned(): YearMonth {
    return this.toYearMonth().plusMonths(1)
}

fun YearMonth.nesteMåned(): YearMonth {
    return this.plusMonths(1)
}

fun inneværendeMåned(): YearMonth {
    return now().toYearMonth()
}

fun senesteDatoAv(dato1: LocalDate, dato2: LocalDate): LocalDate {
    return maxOf(dato1, dato2)
}

fun LocalDate.sisteDagIMåned(): LocalDate {
    return YearMonth.from(this).atEndOfMonth()
}

fun LocalDate.erSenereEnnPåfølgendeDag(tidligereDato: LocalDate): Boolean {
    return this.minusDays(1).isAfter(tidligereDato)
}

fun LocalDate.førsteDagINesteMåned() = this.plusMonths(1).withDayOfMonth(1)
fun LocalDate.førsteDagIInneværendeMåned() = this.withDayOfMonth(1)

fun LocalDate.erSenereEnnNesteMåned() : Boolean = this.isAfter(now().sisteDagINesteMåned())

fun LocalDate.erSenereEnnInneværendeMåned() : Boolean = this.isAfter(now().sisteDagIMåned())

fun LocalDate.erFraInneværendeMåned(now: LocalDate = now()): Boolean {
    val førsteDatoInneværendeMåned = now.withDayOfMonth(1)
    val førsteDatoNesteMåned = førsteDatoInneværendeMåned.plusMonths(1)
    return this.isSameOrAfter(førsteDatoInneværendeMåned) && isBefore(førsteDatoNesteMåned)
}

fun LocalDate.erFraInneværendeEllerForrigeMåned(now: LocalDate = now()): Boolean {
    val førsteDatoForrigeMåned = now.withDayOfMonth(1).minusMonths(1)
    val førsteDatoNesteMåned = førsteDatoForrigeMåned.plusMonths(2)
    return this.isSameOrAfter(førsteDatoForrigeMåned) && isBefore(førsteDatoNesteMåned)
}

fun YearMonth.isSameOrAfter(toCompare: YearMonth): Boolean {
    return this.isAfter(toCompare) || this == toCompare
}

fun LocalDate.isSameOrBefore(toCompare: LocalDate): Boolean {
    return this.isBefore(toCompare) || this == toCompare
}

fun LocalDate.isSameOrAfter(toCompare: LocalDate): Boolean {
    return this.isAfter(toCompare) || this == toCompare
}

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
data class MånedPeriode(val fom: YearMonth, val tom: YearMonth)


fun VilkårResultat.erEtterfølgendePeriode(other: VilkårResultat): Boolean {
    return (other.toPeriode().fom.monthValue - this.toPeriode().tom.monthValue <= 1) &&
           this.toPeriode().tom.year == other.toPeriode().fom.year
}

private fun lagOgValiderPeriodeFraVilkår(periodeFom: LocalDate?,
                                         periodeTom: LocalDate?,
                                         erEksplisittAvslagPåSøknad: Boolean? = null): Periode {
    return when {
        periodeFom !== null -> {
            Periode(fom = periodeFom,
                    tom = periodeTom ?: TIDENES_ENDE)
        }
        erEksplisittAvslagPåSøknad == true && periodeTom == null -> {
            Periode(fom = TIDENES_MORGEN,
                    tom = TIDENES_ENDE)
        }
        else -> {
            throw Feil("Ugyldig periode. Periode må ha t.o.m.-dato eller være et avslag uten datoer.")
        }
    }
}

fun RestVilkårResultat.toPeriode(): Periode = lagOgValiderPeriodeFraVilkår(this.periodeFom,
                                                                           this.periodeTom,
                                                                           this.erEksplisittAvslagPåSøknad)

fun VilkårResultat.toPeriode(): Periode = lagOgValiderPeriodeFraVilkår(this.periodeFom,
                                                                       this.periodeTom,
                                                                       this.erEksplisittAvslagPåSøknad)

fun DatoIntervallEntitet.erInnenfor(dato: LocalDate): Boolean {
    return when {
        fom == null && tom == null -> true
        fom == null -> dato.isSameOrBefore(tom!!)
        tom == null -> dato.isSameOrAfter(fom)
        else -> dato.isSameOrAfter(fom) && dato.isSameOrBefore(tom)
    }
}

fun maksimum(periodeFomSoker: LocalDate?, periodeFomBarn: LocalDate?): LocalDate {
    if (periodeFomSoker == null && periodeFomBarn == null) {
        error("Både søker og barn kan ikke ha null i periodeFom-dato")
    }

    return maxOf(periodeFomSoker ?: LocalDate.MIN, periodeFomBarn ?: LocalDate.MIN)
}

fun minimum(periodeTomSoker: LocalDate?, periodeTomBarn: LocalDate?): LocalDate {
    if (periodeTomSoker == null && periodeTomBarn == null) {
        error("Både søker og barn kan ikke ha null i periodeTom-dato")
    }

    return minOf(periodeTomBarn ?: LocalDate.MAX, periodeTomSoker ?: LocalDate.MAX)
}

fun slåSammenOverlappendePerioder(input: Collection<DatoIntervallEntitet>): List<DatoIntervallEntitet> {
    val map: NavigableMap<LocalDate, LocalDate?> =
            TreeMap()
    for (periode in input) {
        if (periode.fom != null
            && (!map.containsKey(periode.fom) || periode.tom == null || periode.tom.isAfter(map[periode.fom]))) {
            map[periode.fom] = periode.tom
        }
    }
    val result = mutableListOf<DatoIntervallEntitet>()
    var prevIntervall: DatoIntervallEntitet? = null
    for ((key, value) in map) {
        prevIntervall = if (prevIntervall != null && prevIntervall.erInnenfor(key)) {
            val fom = prevIntervall.fom
            val tom = if (prevIntervall.tom == null) {
                null
            } else {
                if (value != null && prevIntervall.tom!!.isAfter(value)) {
                    prevIntervall.tom
                } else {
                    value
                }
            }
            result.remove(prevIntervall)
            val nyttIntervall = DatoIntervallEntitet(fom, tom)
            result.add(nyttIntervall)
            nyttIntervall
        } else {
            val nyttIntervall = DatoIntervallEntitet(key, value)
            result.add(nyttIntervall)
            nyttIntervall
        }
    }
    return result
}


