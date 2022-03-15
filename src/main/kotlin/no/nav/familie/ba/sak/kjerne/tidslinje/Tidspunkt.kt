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
        else
            dato.compareTo(other.dato) // MÅNED-presisjon settes som siste dag i måneden.

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
            Uendelighet.FREMTID -> "-->"
            else -> ""
        } + when (presisjon) {
            TidspunktPresisjon.MÅNED -> this.dato.toYearMonth().toString()
            else -> this.dato.toString()
        }
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
            start.tilMåned().somEndelig(),
            endInclusive.tilMåned().somEndelig()
        )

    override fun toString(): String =
        "$start - $endInclusive"

    companion object {
        private class TidspunktIterator(
            startTidspunkt: Tidspunkt,
            val tilOgMedTidspunkt: Tidspunkt
        ) : Iterator<Tidspunkt> {

            private var gjeldendeTidspunkt = startTidspunkt

            override fun hasNext() =
                gjeldendeTidspunkt.neste() <= tilOgMedTidspunkt.neste()

            override fun next(): Tidspunkt {
                val next = gjeldendeTidspunkt
                gjeldendeTidspunkt = gjeldendeTidspunkt.neste()
                return next
            }
        }

        val NULL = Tidsrom(Tidspunkt(LocalDate.MAX), Tidspunkt(LocalDate.MIN))
    }
}

operator fun Tidspunkt.rangeTo(tilOgMed: Tidspunkt) =
    Tidsrom(this, tilOgMed)

fun <T> Periode<T>.erInnenforTidsrom(tidsrom: Tidsrom) =
    fom <= tidsrom.start && tom >= tidsrom.endInclusive

fun <T> Periode<T>.erEnDelAvTidsrom(tidsrom: Tidsrom) =
    fom <= tidsrom.endInclusive && tom >= tidsrom.start
