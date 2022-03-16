package no.nav.familie.ba.sak.kjerne.eøs.temaperiode

import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import java.time.LocalDate
import java.time.YearMonth

data class Tidspunkt(
    private val dato: LocalDate,
    private val presisjon: TidspunktPresisjon = TidspunktPresisjon.DAG,
    private val uendelighet: Uendelighet = Uendelighet.INGEN
) : Comparable<Tidspunkt> {
    constructor(tidspunkt: YearMonth, toLocalDate: (måned: YearMonth) -> LocalDate = { it.atEndOfMonth() }) :
        this(toLocalDate(tidspunkt), presisjon = TidspunktPresisjon.MÅNED, uendelighet = Uendelighet.INGEN)

    constructor(
        tidspunkt: YearMonth,
        uendelighet: Uendelighet = Uendelighet.INGEN,
        toLocalDate: (måned: YearMonth) -> LocalDate = { it.atEndOfMonth() }
    ) :
        this(toLocalDate(tidspunkt), presisjon = TidspunktPresisjon.MÅNED, uendelighet = uendelighet)

    fun tilFørsteDagIMåneden(): Tidspunkt =
        Tidspunkt(dato.withDayOfMonth(1), TidspunktPresisjon.DAG, this.uendelighet)

    fun tilSisteDagIMåneden(): Tidspunkt =
        Tidspunkt(dato.sisteDagIMåned(), TidspunktPresisjon.DAG, this.uendelighet)

    fun tilDagIMåned(dag: Int): Tidspunkt =
        Tidspunkt(dato.withDayOfMonth(dag), TidspunktPresisjon.DAG, this.uendelighet)

    fun tilMåned() = Tidspunkt(dato.sisteDagIMåned(), TidspunktPresisjon.MÅNED, this.uendelighet)

    fun tilNærmesteHeleMåned(seFremover: Boolean = true): Tidspunkt {
        if (dato.sisteDagIMåned() == dato)
            return tilMåned()
        else
            return copy(dato.plusMonths(if (seFremover) 1 else -1)).tilMåned()
    }

    fun tilLocalDateEllerNull(): LocalDate? {
        if (uendelighet == Uendelighet.INGEN && presisjon == TidspunktPresisjon.DAG)
            return dato
        else
            return null
    }

    fun tilLocalDate(): LocalDate {
        if (uendelighet != Uendelighet.INGEN)
            throw IllegalStateException("Tidspunktet er uendelig")
        if (presisjon != TidspunktPresisjon.DAG)
            throw IllegalStateException("Tidspunktet har presisjon $presisjon")

        return dato
    }

    fun tilYearMonthEllerNull(): YearMonth? {
        if (uendelighet == Uendelighet.INGEN)
            return dato.toYearMonth()
        else
            return null
    }

    fun tilYearMonth(): YearMonth {
        if (uendelighet != Uendelighet.INGEN)
            throw IllegalStateException("Tidspunktet er uendelig")

        return dato.toYearMonth()
    }

    fun hopp(antallSteg: Long) = when (this.presisjon) {
        TidspunktPresisjon.DAG -> this.copy(dato = this.dato.plusDays(antallSteg))
        TidspunktPresisjon.MÅNED -> this.copy(dato = this.dato.plusMonths(antallSteg))
    }

    fun neste() = when (this.presisjon) {
        TidspunktPresisjon.DAG -> this.copy(dato = this.dato.plusDays(1))
        TidspunktPresisjon.MÅNED -> this.copy(dato = this.dato.plusMonths(1))
    }

    fun forrige() = when (this.presisjon) {
        TidspunktPresisjon.DAG -> this.copy(dato = this.dato.minusDays(1))
        TidspunktPresisjon.MÅNED -> this.copy(dato = this.dato.minusMonths(1))
    }

    fun erRettFør(tidspunkt: Tidspunkt): Boolean {
        if (this.harSammePresisjon(tidspunkt))
            return this.neste() == tidspunkt
        else if (this.presisjon == TidspunktPresisjon.DAG &&
            this.tilMåned() < tidspunkt && this.neste().tilMåned() == tidspunkt
        )
            return true
        else return this.presisjon == TidspunktPresisjon.MÅNED && this.neste().tilFørsteDagIMåneden() == tidspunkt
    }

    override fun compareTo(other: Tidspunkt) =
        if (this.uendelighet == Uendelighet.FORTID && other.uendelighet != Uendelighet.FORTID)
            -1
        else if (this.uendelighet == Uendelighet.FREMTID && other.uendelighet != Uendelighet.FREMTID)
            1
        else if (other.uendelighet == Uendelighet.FORTID)
            1
        else if (other.uendelighet == Uendelighet.FREMTID)
            -1
        else if (this.presisjon != other.presisjon || this.presisjon == TidspunktPresisjon.MÅNED)
            this.dato.toYearMonth().compareTo(other.dato.toYearMonth())
        else
            this.dato.compareTo(other.dato)

    fun harSammePresisjon(tidspunkt: Tidspunkt): Boolean {
        return this.presisjon == tidspunkt.presisjon
    }

    fun harPresisjonDag(): Boolean = presisjon == TidspunktPresisjon.DAG
    private fun harPresisjonMåned(): Boolean = this.presisjon == TidspunktPresisjon.MÅNED
    fun erUendeligLengeSiden(): Boolean = uendelighet == Uendelighet.FORTID
    fun erUendeligLengeTil(): Boolean = uendelighet == Uendelighet.FREMTID
    fun somEndelig(): Tidspunkt = this.copy(uendelighet = Uendelighet.INGEN)

    override fun toString(): String {
        return when (uendelighet) {
            Uendelighet.FORTID -> "<--"
            else -> ""
        } + when (presisjon) {
            TidspunktPresisjon.MÅNED -> this.dato.toYearMonth().toString()
            else -> this.dato.toString()
        } + when (uendelighet) {
            Uendelighet.FREMTID -> "-->"
            else -> ""
        }
    }

    fun somUendeligLengeSiden(): Tidspunkt = this.copy(uendelighet = Uendelighet.FORTID)
    fun somUendeligLengeTil(): Tidspunkt = this.copy(uendelighet = Uendelighet.FREMTID)
    fun ikkeUendeligFremtid(): Tidspunkt {
        return if (uendelighet == Uendelighet.FREMTID) {
            somEndelig()
        } else
            this
    }

    fun ikkeUendeligFortid(): Tidspunkt {
        return if (uendelighet == Uendelighet.FORTID) {
            somEndelig()
        } else
            this
    }

    companion object {
        fun uendeligLengeSiden(dato: LocalDate) = Tidspunkt(dato, uendelighet = Uendelighet.FORTID)
        fun uendeligLengeSiden(måned: YearMonth) = Tidspunkt(måned, Uendelighet.FORTID)
        fun uendeligLengeTil(dato: LocalDate) = Tidspunkt(dato, uendelighet = Uendelighet.FREMTID)
        fun uendeligLengeTil(måned: YearMonth) = Tidspunkt(måned, Uendelighet.FREMTID)
    }

    enum class Uendelighet {
        INGEN,
        FORTID,
        FREMTID
    }

    enum class TidspunktPresisjon {
        DAG,
        MÅNED
    }
}

data class Tidsrom(
    override val start: Tidspunkt,
    override val endInclusive: Tidspunkt
) : Iterable<Tidspunkt>,
    ClosedRange<Tidspunkt> {

    override fun iterator(): Iterator<Tidspunkt> =
        // TODO: Dette er ikke helt bra!
        TidspunktIterator(
            start.tilMåned(),
            endInclusive.tilMåned()
        )

    override fun toString(): String =
        "$start - $endInclusive"

    companion object {
        private class TidspunktIterator(
            val startTidspunkt: Tidspunkt,
            val tilOgMedTidspunkt: Tidspunkt
        ) : Iterator<Tidspunkt> {

            private var gjeldendeTidspunkt = startTidspunkt.somEndelig()

            override fun hasNext() =
                gjeldendeTidspunkt.neste() <= tilOgMedTidspunkt.neste().somEndelig()

            override fun next(): Tidspunkt {
                val next = gjeldendeTidspunkt
                gjeldendeTidspunkt = gjeldendeTidspunkt.neste()

                return if (next.compareTo(tilOgMedTidspunkt.somEndelig()) == 0)
                    tilOgMedTidspunkt
                else if (next.compareTo(startTidspunkt.somEndelig()) == 0)
                    startTidspunkt
                else
                    next
            }
        }

        val NULL = Tidsrom(Tidspunkt(LocalDate.MAX), Tidspunkt(LocalDate.MIN))
    }
}

operator fun Tidspunkt.rangeTo(tilOgMed: Tidspunkt) =
    Tidsrom(this, tilOgMed)

fun Iterable<Tidspunkt>.minste(): Tidspunkt {
    val iterator = this.iterator()
    if (!iterator.hasNext()) throw NoSuchElementException()
    var minValue = iterator.next()
    while (iterator.hasNext()) {
        val tidspunkt = iterator.next()
        if (tidspunkt.erUendeligLengeSiden() && minValue.somEndelig() < tidspunkt.somEndelig())
            minValue = minValue.somUendeligLengeSiden()
        else if (minValue > tidspunkt) {
            minValue = tidspunkt
        }
    }
    return minValue
}

fun Iterable<Tidspunkt>.største(): Tidspunkt {
    val iterator = this.iterator()
    if (!iterator.hasNext()) throw NoSuchElementException()
    var maxValue = iterator.next()
    while (iterator.hasNext()) {
        val tidspunkt = iterator.next()
        if (tidspunkt.erUendeligLengeTil() && maxValue.somEndelig() > tidspunkt.somEndelig())
            maxValue = maxValue.somUendeligLengeTil()
        else if (maxValue < tidspunkt) {
            maxValue = tidspunkt
        }
    }
    return maxValue
}

fun <T> Periode<T>.erInnenforTidsrom(tidsrom: Tidsrom) =
    fom <= tidsrom.start && tom >= tidsrom.endInclusive

fun <T> Periode<T>.erEnDelAvTidsrom(tidsrom: Tidsrom) =
    fom <= tidsrom.endInclusive && tom >= tidsrom.start
