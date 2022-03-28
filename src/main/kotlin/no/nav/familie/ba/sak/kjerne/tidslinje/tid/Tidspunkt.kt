package no.nav.familie.ba.sak.kjerne.tidslinje.tid

import no.nav.familie.ba.sak.common.toYearMonth
import java.time.LocalDate
import java.time.YearMonth

val PRAKTISK_SENESTE_DAG = LocalDate.of(2499, 12, 31)
val PRAKTISK_TIDLIGSTE_DAG = LocalDate.of(1900, 1, 1)

internal val månedTilDagKonverterer: (YearMonth) -> LocalDate = { it.atEndOfMonth() }
internal val dagTilMånedKonverterer: (LocalDate) -> YearMonth = { it.toYearMonth() }

internal enum class Uendelighet {
    INGEN,
    FORTID,
    FREMTID
}

interface Tidsenhet
class Dag : Tidsenhet
class Måned : Tidsenhet

abstract class Tidspunkt<T : Tidsenhet> internal constructor(
    private val uendelighet: Uendelighet
) : Comparable<Tidspunkt<T>> {
    abstract fun tilFørsteDagIMåneden(): DagTidspunkt
    abstract fun tilSisteDagIMåneden(): DagTidspunkt
    abstract fun tilInneværendeMåned(): MånedTidspunkt
    abstract fun tilLocalDateEllerNull(): LocalDate?
    abstract fun tilLocalDate(): LocalDate
    abstract fun tilYearMonthEllerNull(): YearMonth?
    abstract fun tilYearMonth(): YearMonth

    abstract fun flytt(tidsenheter: Long): Tidspunkt<T>
    fun neste() = flytt(1)
    fun forrige() = flytt(-1)

    fun erRettFør(tidspunkt: Tidspunkt<T>) = neste() == tidspunkt
    fun erEndelig(): Boolean = uendelighet == Uendelighet.INGEN
    fun erUendeligLengeSiden(): Boolean = uendelighet == Uendelighet.FORTID
    fun erUendeligLengeTil(): Boolean = uendelighet == Uendelighet.FREMTID

    abstract fun somEndelig(): Tidspunkt<T>
    abstract fun somUendeligLengeSiden(): Tidspunkt<T>
    abstract fun somUendeligLengeTil(): Tidspunkt<T>
    abstract fun somFraOgMed(): Tidspunkt<T>
    abstract fun somFraOgMed(dato: LocalDate): Tidspunkt<T>
    abstract fun somTilOgMed(): Tidspunkt<T>
    abstract fun somTilOgMed(dato: LocalDate): Tidspunkt<T>

    companion object {
        fun uendeligLengeSiden(dato: LocalDate) = DagTidspunkt(dato, uendelighet = Uendelighet.FORTID)
        fun uendeligLengeSiden(måned: YearMonth) = MånedTidspunkt(måned, Uendelighet.FORTID)
        fun uendeligLengeTil(dato: LocalDate) = DagTidspunkt(dato, uendelighet = Uendelighet.FREMTID)
        fun uendeligLengeTil(måned: YearMonth) = MånedTidspunkt(måned, Uendelighet.FREMTID)
        fun fraOgMed(fraOgMed: LocalDate?, praktiskMinsteFraOgMed: LocalDate): DagTidspunkt =
            if (fraOgMed == null || fraOgMed < PRAKTISK_TIDLIGSTE_DAG)
                uendeligLengeSiden(maxOf(praktiskMinsteFraOgMed, PRAKTISK_TIDLIGSTE_DAG))
            else
                DagTidspunkt(fraOgMed, Uendelighet.INGEN)

        fun tilOgMed(tilOgMed: LocalDate?, praktiskStørsteTilOgMed: LocalDate): DagTidspunkt =
            if (tilOgMed == null || tilOgMed > PRAKTISK_SENESTE_DAG)
                uendeligLengeTil(minOf(praktiskStørsteTilOgMed, PRAKTISK_SENESTE_DAG))
            else
                DagTidspunkt(tilOgMed, Uendelighet.INGEN)

        fun med(dato: LocalDate) = DagTidspunkt(dato, Uendelighet.INGEN)
        fun med(måned: YearMonth) = MånedTidspunkt(måned, Uendelighet.INGEN)
    }

    // Betrakter to uendeligheter som like, selv underliggende tidspunkt kan være forskjellig
    override fun compareTo(other: Tidspunkt<T>) =
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
            sammenliknMed(other)

    protected abstract fun sammenliknMed(tidspunkt: Tidspunkt<T>): Int
}

fun <T : Tidsenhet> størsteAv(t1: Tidspunkt<T>, t2: Tidspunkt<T>): Tidspunkt<T> =
    if (t1.erUendeligLengeTil() && t2.erEndelig() && t1.somEndelig() <= t2)
        t2.neste().somUendeligLengeTil()
    else if (t2.erUendeligLengeTil() && t1.erEndelig() && t2.somEndelig() <= t1)
        t1.neste().somUendeligLengeTil()
    else if (t1.erUendeligLengeTil() || t2.erUendeligLengeTil())
        maxOf(t1.somEndelig(), t2.somEndelig()).somUendeligLengeTil()
    else
        maxOf(t1, t2)

fun <T : Tidsenhet> minsteAv(t1: Tidspunkt<T>, t2: Tidspunkt<T>): Tidspunkt<T> =
    if (t1.erUendeligLengeSiden() && t2.erEndelig() && t1.somEndelig() >= t2)
        t2.forrige().somUendeligLengeSiden()
    else if (t2.erUendeligLengeSiden() && t1.erEndelig() && t2.somEndelig() >= t1)
        t1.forrige().somUendeligLengeSiden()
    else if (t1.erUendeligLengeSiden() || t2.erUendeligLengeSiden())
        minOf(t1.somEndelig(), t2.somEndelig()).somUendeligLengeSiden()
    else
        minOf(t1, t2)

fun <T : Tidsenhet> Iterable<Tidspunkt<T>>.størsteEllerNull() =
    this.reduceOrNull { acc, neste ->
        størsteAv(acc, neste)
    }

fun <T : Tidsenhet> Iterable<Tidspunkt<T>>.minsteEllerNull() =
    this.reduceOrNull { acc, neste -> minsteAv(acc, neste) }

fun LocalDate?.tilTidspunktEllerDefault(default: () -> LocalDate) =
    this?.let { DagTidspunkt(this, Uendelighet.INGEN) } ?: DagTidspunkt(default(), Uendelighet.INGEN)

fun YearMonth.tilTidspunkt() = MånedTidspunkt(this, Uendelighet.INGEN)
