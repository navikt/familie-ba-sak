package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.ekstern.restDomene.VilkårResultatDto
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.tidslinje.PRAKTISK_SENESTE_DAG
import no.nav.familie.tidslinje.PRAKTISK_TIDLIGSTE_DAG
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.YearMonth
import java.time.format.DateTimeFormatter

val TIDENES_MORGEN = LocalDate.MIN
val TIDENES_ENDE = LocalDate.MAX

private val FORMAT_DATE_DDMMYY = DateTimeFormatter.ofPattern("ddMMyy", nbLocale)
private val FORMAT_DATE_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd", nbLocale)
private val FORMAT_DATO_NORSK_KORT_ÅR = DateTimeFormatter.ofPattern("dd.MM.yy", nbLocale)
private val FORMAT_DATO_NORSK = DateTimeFormatter.ofPattern("dd.MM.yyyy", nbLocale)
private val FORMAT_DATO_MÅNED_ÅR_KORT = DateTimeFormatter.ofPattern("MM.yy", nbLocale)
private val FORMAT_DATO_DAG_MÅNED_ÅR = DateTimeFormatter.ofPattern("d. MMMM yyyy", nbLocale)
private val FORMAT_DATO_MÅNED_ÅR = DateTimeFormatter.ofPattern("MMMM yyyy", nbLocale)
private val FORMAT_DATO_MÅNED_ÅR_MEDIUM = DateTimeFormatter.ofPattern("MMM yy", nbLocale)
private val FORMAT_DATO_KORT_MÅNED_LANGT_ÅR = DateTimeFormatter.ofPattern("MM.yyyy", nbLocale)

fun LocalDate.tilddMMyy() = this.format(FORMAT_DATE_DDMMYY)

fun LocalDate.tilyyyyMMdd() = this.format(FORMAT_DATE_ISO)

fun LocalDate.tilKortString() = this.format(FORMAT_DATO_NORSK_KORT_ÅR)

fun LocalDate.tilddMMyyyy() = this.format(FORMAT_DATO_NORSK)

fun YearMonth.tilKortString() = this.format(FORMAT_DATO_MÅNED_ÅR_KORT)

fun LocalDate.tilDagMånedÅr() = this.format(FORMAT_DATO_DAG_MÅNED_ÅR)

fun LocalDate.tilMånedÅr() = this.format(FORMAT_DATO_MÅNED_ÅR)

fun YearMonth.tilMånedÅr() = this.format(FORMAT_DATO_MÅNED_ÅR)

fun YearMonth.tilMånedÅrMedium() = this.format(FORMAT_DATO_MÅNED_ÅR_MEDIUM)

fun YearMonth.tilKortMånedLangtÅr() = this.format(FORMAT_DATO_KORT_MÅNED_LANGT_ÅR)

fun erBack2BackIMånedsskifte(
    tilOgMed: LocalDate?,
    fraOgMed: LocalDate?,
): Boolean =
    tilOgMed?.erDagenFør(fraOgMed) == true &&
        tilOgMed.toYearMonth() != fraOgMed?.toYearMonth()

fun LocalDate.sisteDagIForrigeMåned(): LocalDate {
    val sammeDagForrigeMåned = this.minusMonths(1)
    return sammeDagForrigeMåned.sisteDagIMåned()
}

fun LocalDate.toYearMonth() = YearMonth.from(this)

fun YearMonth.toLocalDate() = LocalDate.of(this.year, this.month, 1)

fun YearMonth.førsteDagIInneværendeMåned() = this.atDay(1)

fun YearMonth.sisteDagIInneværendeMåned() = this.atEndOfMonth()

fun LocalDate.forrigeMåned(): YearMonth = this.toYearMonth().minusMonths(1)

fun YearMonth.forrigeMåned(): YearMonth = this.minusMonths(1)

fun LocalDate.nesteMåned(): YearMonth = this.toYearMonth().plusMonths(1)

fun YearMonth.nesteMåned(): YearMonth = this.plusMonths(1)

fun inneværendeMåned(): YearMonth = now().toYearMonth()

fun senesteDatoAv(
    dato1: LocalDate,
    dato2: LocalDate,
): LocalDate = maxOf(dato1, dato2)

fun LocalDate.til18ÅrsVilkårsdato() = this.plusYears(18).minusDays(1)

fun LocalDate.sisteDagIMåned(): LocalDate = YearMonth.from(this).atEndOfMonth()

fun LocalDate.førsteDagINesteMåned() = this.plusMonths(1).withDayOfMonth(1)

fun LocalDate.førsteDagIInneværendeMåned() = this.withDayOfMonth(1)

fun LocalDate.erDagenFør(other: LocalDate?) = other != null && this.plusDays(1).equals(other)

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

fun YearMonth.isSameOrBefore(toCompare: YearMonth): Boolean = this.isBefore(toCompare) || this == toCompare

fun YearMonth.isSameOrAfter(toCompare: YearMonth): Boolean = this.isAfter(toCompare) || this == toCompare

fun LocalDate.isSameOrBefore(toCompare: LocalDate): Boolean = this.isBefore(toCompare) || this == toCompare

fun LocalDate.isSameOrAfter(toCompare: LocalDate): Boolean = this.isAfter(toCompare) || this == toCompare

fun LocalDate.isBetween(toCompare: Periode): Boolean = this.isSameOrAfter(toCompare.fom) && this.isSameOrBefore(toCompare.tom)

fun Periode.overlapperHeltEllerDelvisMed(annenPeriode: Periode) =
    this.fom.isBetween(annenPeriode) ||
        this.tom.isBetween(annenPeriode) ||
        annenPeriode.fom.isBetween(this) ||
        annenPeriode.tom.isBetween(this)

fun MånedPeriode.inkluderer(yearMonth: YearMonth) = yearMonth >= this.fom && yearMonth <= this.tom

fun MånedPeriode.overlapperHeltEllerDelvisMed(annenPeriode: MånedPeriode) =
    this.inkluderer(annenPeriode.fom) ||
        this.inkluderer(annenPeriode.tom) ||
        annenPeriode.inkluderer(this.fom) ||
        annenPeriode.inkluderer(this.tom)

fun MånedPeriode.erMellom(annenPeriode: MånedPeriode) = annenPeriode.inkluderer(this.fom) && annenPeriode.inkluderer(this.tom)

fun Periode.kanErstatte(other: Periode): Boolean = this.fom.isSameOrBefore(other.fom) && this.tom.isSameOrAfter(other.tom)

fun LocalDate.erMellomIkkeLik(other: Periode): Boolean = this.isAfter(other.fom) && this.isBefore(other.tom)

fun Periode.kanSplitte(other: Periode): Boolean =
    this.fom.erMellomIkkeLik(other) &&
        this.tom.erMellomIkkeLik(other) &&
        (this.tom != TIDENES_ENDE || other.tom != TIDENES_ENDE)

fun Periode.kanFlytteFom(other: Periode): Boolean = this.fom.isSameOrBefore(other.fom) && this.tom.isBetween(other)

fun Periode.kanFlytteTom(other: Periode): Boolean = this.fom.isBetween(other) && this.tom.isSameOrAfter(other.tom)

fun Periode.tilMånedPeriode(): MånedPeriode = MånedPeriode(fom = this.fom.toYearMonth(), tom = this.tom.toYearMonth())

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
)

data class MånedPeriode(
    val fom: YearMonth,
    val tom: YearMonth,
)

fun VilkårResultat.erEtterfølgendePeriode(other: VilkårResultat): Boolean =
    (other.toPeriode().fom.monthValue - this.toPeriode().tom.monthValue <= 1) &&
        this.toPeriode().tom.year == other.toPeriode().fom.year

fun lagOgValiderPeriodeFraVilkår(
    periodeFom: LocalDate?,
    periodeTom: LocalDate?,
    erEksplisittAvslagPåSøknad: Boolean? = null,
): Periode =
    when {
        periodeFom !== null -> {
            Periode(
                fom = periodeFom,
                tom = periodeTom ?: TIDENES_ENDE,
            )
        }

        erEksplisittAvslagPåSøknad == true && periodeTom == null -> {
            Periode(
                fom = TIDENES_MORGEN,
                tom = TIDENES_ENDE,
            )
        }

        else -> {
            throw FunksjonellFeil("Ugyldig periode. Periode må ha t.o.m.-dato eller være et avslag uten datoer.")
        }
    }

fun VilkårResultatDto.toPeriode(): Periode =
    lagOgValiderPeriodeFraVilkår(
        this.periodeFom,
        this.periodeTom,
        this.erEksplisittAvslagPåSøknad,
    )

fun VilkårResultat.toPeriode(): Periode =
    lagOgValiderPeriodeFraVilkår(
        this.periodeFom,
        this.periodeTom,
        this.erEksplisittAvslagPåSøknad,
    )

fun LocalDate.erInnenfor(periode: DatoIntervallEntitet): Boolean =
    when {
        periode.fom == null && periode.tom == null -> true
        periode.fom == null -> isSameOrBefore(periode.tom!!)
        periode.tom == null -> isSameOrAfter(periode.fom)
        else -> isSameOrAfter(periode.fom) && isSameOrBefore(periode.tom)
    }

class YearMonthIterator(
    startMåned: YearMonth,
    val tilOgMedMåned: YearMonth,
    val hoppMåneder: Long,
) : Iterator<YearMonth> {
    private var gjeldendeMåned = startMåned

    override fun hasNext() =
        if (hoppMåneder > 0) {
            gjeldendeMåned.plusMonths(hoppMåneder) <= tilOgMedMåned.plusMonths(1)
        } else if (hoppMåneder < 0) {
            gjeldendeMåned.plusMonths(hoppMåneder) >= tilOgMedMåned.plusMonths(-1)
        } else {
            throw Feil("Steglengde kan ikke være null")
        }

    override fun next(): YearMonth {
        val next = gjeldendeMåned
        gjeldendeMåned = gjeldendeMåned.plusMonths(hoppMåneder)
        return next
    }
}

class YearMonthProgression(
    override val start: YearMonth,
    override val endInclusive: YearMonth,
    val hoppMåneder: Long = 1,
) : Iterable<YearMonth>,
    ClosedRange<YearMonth> {
    override fun iterator(): Iterator<YearMonth> = YearMonthIterator(start, endInclusive, hoppMåneder)

    infix fun step(måneder: Long) = YearMonthProgression(start, endInclusive, måneder)
}

operator fun YearMonth?.rangeTo(andre: YearMonth?) = YearMonthProgression(this ?: PRAKTISK_TIDLIGSTE_DAG.toYearMonth(), andre ?: PRAKTISK_SENESTE_DAG.toYearMonth())

class LocalDateIterator(
    private var currentDate: LocalDate,
    private val endInclusive: LocalDate,
) : Iterator<LocalDate> {
    override fun hasNext() = currentDate <= endInclusive

    override fun next(): LocalDate {
        val next = currentDate
        currentDate = currentDate.plusDays(1)
        return next
    }
}

class LocalDateProgression(
    override val start: LocalDate,
    override val endInclusive: LocalDate,
) : Iterable<LocalDate>,
    ClosedRange<LocalDate> {
    override fun iterator(): Iterator<LocalDate> = LocalDateIterator(start, endInclusive)
}

operator fun LocalDate?.rangeTo(andre: LocalDate?) = LocalDateProgression(this ?: PRAKTISK_TIDLIGSTE_DAG, andre ?: PRAKTISK_SENESTE_DAG)
