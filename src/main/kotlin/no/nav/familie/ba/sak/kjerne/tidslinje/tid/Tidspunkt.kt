package no.nav.familie.ba.sak.kjerne.tidslinje.tid

import no.nav.familie.ba.sak.common.toYearMonth
import java.time.LocalDate
import java.time.YearMonth

val PRAKTISK_SENESTE_DAG = LocalDate.of(2499, 12, 31)
val PRAKTISK_TIDLIGSTE_DAG = LocalDate.of(1900, 1, 1)

internal val månedTilDagKonverterer: (YearMonth) -> LocalDate = { it.atEndOfMonth() }
internal val dagTilMånedKonverterer: (LocalDate) -> YearMonth = { it.toYearMonth() }

enum class Uendelighet {
    INGEN,
    FORTID,
    FREMTID
}

interface Tidspunkt : Comparable<Tidspunkt> {
    val uendelighet: Uendelighet

    fun tilFørsteDagIMåneden(): DagTidspunkt
    fun tilSisteDagIMåneden(): DagTidspunkt
    fun tilDagIMåned(dag: Int): DagTidspunkt
    fun tilDag(månedTilDagMapper: (YearMonth) -> LocalDate): DagTidspunkt
    fun tilInneværendeMåned(): MånedTidspunkt
    fun tilLocalDateEllerNull(): LocalDate?
    fun tilLocalDate(): LocalDate
    fun tilYearMonthEllerNull(): YearMonth?
    fun tilYearMonth(): YearMonth

    fun flytt(tidsenheter: Long): Tidspunkt
    fun neste() = flytt(1)
    fun forrige() = flytt(-1)

    fun erRettFør(tidspunkt: Tidspunkt) = neste() == tidspunkt
    fun erEndelig(): Boolean = uendelighet == Uendelighet.INGEN
    fun erUendeligLengeSiden(): Boolean = uendelighet == Uendelighet.FORTID
    fun erUendeligLengeTil(): Boolean = uendelighet == Uendelighet.FREMTID

    fun somEndelig(): Tidspunkt
    fun somUendeligLengeSiden(): Tidspunkt
    fun somUendeligLengeTil(): Tidspunkt
    fun somFraOgMed(): Tidspunkt
    fun somTilOgMed(): Tidspunkt

    fun erDag(): Boolean
    fun erMåned() = !erDag()

    companion object {
        fun uendeligLengeSiden(dato: LocalDate) = DagTidspunkt(dato, uendelighet = Uendelighet.FORTID)
        fun uendeligLengeSiden(måned: YearMonth) = MånedTidspunkt(måned, Uendelighet.FORTID)
        fun uendeligLengeTil(dato: LocalDate) = DagTidspunkt(dato, uendelighet = Uendelighet.FREMTID)
        fun uendeligLengeTil(måned: YearMonth) = MånedTidspunkt(måned, Uendelighet.FREMTID)
        fun fraOgMed(fraOgMed: LocalDate?, praktiskMinsteFraOgMed: LocalDate) =
            if (fraOgMed == null || fraOgMed < PRAKTISK_TIDLIGSTE_DAG)
                uendeligLengeSiden(maxOf(praktiskMinsteFraOgMed, PRAKTISK_TIDLIGSTE_DAG))
            else
                DagTidspunkt(fraOgMed, Uendelighet.INGEN)

        fun tilOgMed(tilOgMed: LocalDate?, praktiskStørsteTilOgMed: LocalDate) =
            if (tilOgMed == null || tilOgMed > PRAKTISK_SENESTE_DAG)
                uendeligLengeTil(minOf(praktiskStørsteTilOgMed, PRAKTISK_SENESTE_DAG))
            else
                DagTidspunkt(tilOgMed, Uendelighet.INGEN)

        fun med(dato: LocalDate) = DagTidspunkt(dato, Uendelighet.INGEN)
        fun med(måned: YearMonth) = MånedTidspunkt(måned, Uendelighet.INGEN)
        fun iDag() = DagTidspunkt(LocalDate.now(), Uendelighet.INGEN)
    }

    // Betrakter to uendeligheter som like, selv underliggende tidspunkt kan være forskjellig
    override fun compareTo(other: Tidspunkt) =
        if (this.uendelighet == Uendelighet.FORTID && other.uendelighet == Uendelighet.FORTID)
            0
        else if (this.uendelighet == Uendelighet.FREMTID && other.uendelighet == Uendelighet.FREMTID)
            0
        else if (this.uendelighet == Uendelighet.FORTID && other.uendelighet != Uendelighet.FORTID)
            -1
        else if (this.uendelighet == Uendelighet.FREMTID && other.uendelighet != Uendelighet.FREMTID)
            1
        else if (this.uendelighet != Uendelighet.FORTID && other.uendelighet == Uendelighet.FORTID)
            1
        else if (this.uendelighet != Uendelighet.FREMTID && other.uendelighet == Uendelighet.FREMTID)
            -1
        else
            this.tilLocalDate().compareTo(other.tilLocalDate())
}

fun størsteAv(t1: Tidspunkt, t2: Tidspunkt): Tidspunkt =
    if (t1.erUendeligLengeTil() && t2.erEndelig() && t1.somEndelig() <= t2)
        t2.neste().somUendeligLengeTil()
    else if (t2.erUendeligLengeTil() && t1.erEndelig() && t2.somEndelig() <= t1)
        t1.neste().somUendeligLengeTil()
    else if (t1.erUendeligLengeTil() || t2.erUendeligLengeTil())
        maxOf(t1.somEndelig(), t2.somEndelig()).somUendeligLengeTil()
    else
        maxOf(t1, t2)

fun minsteAv(t1: Tidspunkt, t2: Tidspunkt): Tidspunkt =
    if (t1.erUendeligLengeSiden() && t2.erEndelig() && t1.somEndelig() >= t2)
        t2.forrige().somUendeligLengeSiden()
    else if (t2.erUendeligLengeSiden() && t1.erEndelig() && t2.somEndelig() >= t1)
        t1.forrige().somUendeligLengeSiden()
    else if (t1.erUendeligLengeSiden() || t2.erUendeligLengeSiden())
        minOf(t1.somEndelig(), t2.somEndelig()).somUendeligLengeSiden()
    else
        minOf(t1, t2)

fun Iterable<Tidspunkt>.størsteEllerNull() =
    this.reduceOrNull() { acc, neste ->
        størsteAv(acc, neste)
    }

fun Iterable<Tidspunkt>.minsteEllerNull() =
    this.reduceOrNull() { acc, neste -> minsteAv(acc, neste) }

fun LocalDate?.tilTidspunktEllerUendeligLengeSiden(default: () -> LocalDate) =
    this?.let { DagTidspunkt(this, Uendelighet.INGEN) } ?: Tidspunkt.uendeligLengeSiden(default())

fun LocalDate?.tilTidspunktEllerUendeligLengeTil(default: () -> LocalDate) =
    this?.let { DagTidspunkt(this, Uendelighet.INGEN) } ?: Tidspunkt.uendeligLengeTil(default())

fun YearMonth?.tilTidspunktEllerUendeligLengeSiden(default: () -> YearMonth) =
    this?.let { MånedTidspunkt(this, Uendelighet.INGEN) } ?: Tidspunkt.uendeligLengeSiden(default())

fun YearMonth?.tilTidspunktEllerUendeligLengeTil(default: () -> YearMonth) =
    this?.let { MånedTidspunkt(this, Uendelighet.INGEN) } ?: Tidspunkt.uendeligLengeTil(default())
