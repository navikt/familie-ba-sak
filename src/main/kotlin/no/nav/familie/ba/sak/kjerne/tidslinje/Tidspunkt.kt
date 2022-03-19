package no.nav.familie.ba.sak.kjerne.eøs.temaperiode

import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import java.time.LocalDate
import java.time.YearMonth

interface Tidspunkt : Comparable<Tidspunkt> {
    val uendelighet: Uendelighet

    fun tilFørsteDagIMåneden(): DagTidspunkt
    fun tilSisteDagIMåneden(): DagTidspunkt
    fun tilDagIMåned(dag: Int): DagTidspunkt
    fun tilMåned(): MånedTidspunkt
    fun tilDag(): DagTidspunkt
    fun tilLocalDateEllerNull(): LocalDate?
    fun tilLocalDate(): LocalDate
    fun tilYearMonthEllerNull(): YearMonth?
    fun tilYearMonth(): YearMonth

    fun flytt(antallSteg: Long): Tidspunkt
    fun neste() = flytt(1)
    fun forrige() = flytt(-1)

    fun erRettFør(tidspunkt: Tidspunkt) = neste() == tidspunkt
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
            if (fraOgMed == null || fraOgMed == LocalDate.MIN)
                uendeligLengeSiden(praktiskMinsteFraOgMed)
            else
                DagTidspunkt(fraOgMed)

        fun tilOgMed(tilOgMed: LocalDate?, praktiskStørsteTilOgMed: LocalDate) =
            if (tilOgMed == null || tilOgMed == LocalDate.MAX)
                uendeligLengeTil(praktiskStørsteTilOgMed)
            else
                DagTidspunkt(tilOgMed)

        fun med(dato: LocalDate) = DagTidspunkt(dato)
        fun med(måned: YearMonth) = MånedTidspunkt(måned)
    }

    override fun compareTo(other: Tidspunkt) =
        if (this.uendelighet == Uendelighet.FORTID && other.uendelighet != Uendelighet.FORTID)
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

data class DagTidspunkt(
    private val dato: LocalDate,
    override val uendelighet: Uendelighet = Uendelighet.INGEN,
    private val månedTilDagKonverterer: (YearMonth) -> LocalDate = { it.atEndOfMonth() },
    private val dagTilMånedKonverterer: (LocalDate) -> YearMonth = { it.toYearMonth() }
) : Tidspunkt {

    init {
        if (dato == LocalDate.MIN)
            throw IllegalArgumentException("Kan ikke håndtere LocalDate.MIN. Bruk uendeligLengeSiden()")
        else if (dato == LocalDate.MAX)
            throw IllegalArgumentException("Kan ikke håndtere LocalDate.MAX. Bruk uendeligLengeTil()")
    }

    override fun tilFørsteDagIMåneden(): DagTidspunkt {
        return this.copy(dato = dato.withDayOfMonth(1))
    }

    override fun tilSisteDagIMåneden(): DagTidspunkt {
        return this.copy(dato = dato.sisteDagIMåned())
    }

    override fun tilDagIMåned(dag: Int): DagTidspunkt {
        return this.copy(dato = dato.withDayOfMonth(dag))
    }

    override fun tilMåned(): MånedTidspunkt {
        return MånedTidspunkt(dagTilMånedKonverterer(this.dato), uendelighet)
    }

    override fun tilDag() = this

    override fun tilLocalDateEllerNull(): LocalDate? {
        if (uendelighet != Uendelighet.INGEN)
            return null
        else
            return dato
    }

    override fun tilLocalDate(): LocalDate {
        return tilLocalDateEllerNull() ?: throw IllegalStateException("Tidspunkt er uendelig")
    }

    override fun tilYearMonthEllerNull(): YearMonth? {
        return tilLocalDateEllerNull()?.let { dagTilMånedKonverterer(it) }
    }

    override fun tilYearMonth(): YearMonth {
        return dagTilMånedKonverterer(tilLocalDate())
    }

    override fun flytt(antallSteg: Long): Tidspunkt {
        return this.copy(dato = dato.plusDays(antallSteg))
    }

    override fun somEndelig(): Tidspunkt {
        return copy(uendelighet = Uendelighet.INGEN)
    }

    override fun somUendeligLengeSiden(): Tidspunkt {
        return copy(uendelighet = Uendelighet.FORTID)
    }

    override fun somUendeligLengeTil(): Tidspunkt {
        return copy(uendelighet = Uendelighet.FREMTID)
    }

    override fun somFraOgMed(): Tidspunkt {
        return if (uendelighet == Uendelighet.FREMTID)
            somEndelig()
        else
            this
    }

    override fun somTilOgMed(): Tidspunkt {
        return if (uendelighet == Uendelighet.FORTID)
            somEndelig()
        else
            this
    }

    override fun erDag() = true
}

data class MånedTidspunkt(
    private val måned: YearMonth,
    override val uendelighet: Uendelighet = Uendelighet.INGEN,
    private val månedTilDagKonverterer: (YearMonth) -> LocalDate = { it.atEndOfMonth() },
    private val dagTilMånedKonverterer: (LocalDate) -> YearMonth = { it.toYearMonth() }
) : Tidspunkt {
    constructor(tidspunkt: YearMonth) : this(tidspunkt, uendelighet = Uendelighet.INGEN)

    init {
        if (måned == LocalDate.MIN.toYearMonth())
            throw IllegalArgumentException("Kan ikke håndtere YearMonth.MIN. Bruk uendeligLengeSiden()")
        else if (måned == LocalDate.MAX.toYearMonth())
            throw IllegalArgumentException("Kan ikke håndtere YearMonth.MAX. Bruk uendeligLengeTil()")
    }

    override fun tilFørsteDagIMåneden(): DagTidspunkt =
        DagTidspunkt(måned.atDay(1), this.uendelighet)

    override fun tilSisteDagIMåneden(): DagTidspunkt =
        DagTidspunkt(måned.atEndOfMonth(), this.uendelighet)

    override fun tilDagIMåned(dag: Int): DagTidspunkt =
        DagTidspunkt(måned.atDay(dag), this.uendelighet)

    override fun tilMåned() = this
    override fun tilDag() = DagTidspunkt(månedTilDagKonverterer(måned), uendelighet)

    override fun tilLocalDateEllerNull(): LocalDate? =
        tilYearMonthEllerNull()?.let(månedTilDagKonverterer)

    override fun tilLocalDate(): LocalDate = månedTilDagKonverterer(måned)

    override fun tilYearMonthEllerNull(): YearMonth? =
        if (uendelighet != Uendelighet.INGEN)
            null
        else
            måned

    override fun tilYearMonth(): YearMonth =
        if (uendelighet != Uendelighet.INGEN)
            throw IllegalStateException("Tidspunktet er uendelig")
        else
            måned

    override fun flytt(antallSteg: Long) = copy(måned = måned.plusMonths(antallSteg))

    override fun neste() = flytt(1)

    override fun forrige() = flytt(1)

    override fun erRettFør(tidspunkt: Tidspunkt): Boolean = this.neste() == tidspunkt

    override fun somEndelig(): Tidspunkt = this.copy(uendelighet = Uendelighet.INGEN)

    override fun toString(): String {
        return when (uendelighet) {
            Uendelighet.FORTID -> "<--"
            else -> ""
        } + måned + when (uendelighet) {
            Uendelighet.FREMTID -> "-->"
            else -> ""
        }
    }

    override fun somUendeligLengeSiden(): Tidspunkt = this.copy(uendelighet = Uendelighet.FORTID)
    override fun somUendeligLengeTil(): Tidspunkt = this.copy(uendelighet = Uendelighet.FREMTID)
    override fun somFraOgMed(): Tidspunkt {
        return if (uendelighet == Uendelighet.FREMTID) {
            somEndelig()
        } else
            this
    }

    override fun somTilOgMed(): Tidspunkt {
        return if (uendelighet == Uendelighet.FORTID) {
            somEndelig()
        } else
            this
    }

    override fun erDag() = false
}

data class Tidsrom(
    override val start: Tidspunkt,
    override val endInclusive: Tidspunkt
) : Iterable<Tidspunkt>,
    ClosedRange<Tidspunkt> {

    override fun iterator(): Iterator<Tidspunkt> =
        if (start.erDag() || endInclusive.erDag())
            TidspunktIterator(start.tilDag(), endInclusive.tilDag())
        else
            TidspunktIterator(start, endInclusive)

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

                return if (next == tilOgMedTidspunkt.somEndelig())
                    tilOgMedTidspunkt
                else if (next == startTidspunkt.somEndelig())
                    startTidspunkt
                else
                    next
            }
        }

        val NULL = Tidspunkt.med(LocalDate.now())..Tidspunkt.med(LocalDate.now().minusDays(2))
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

enum class Uendelighet {
    INGEN,
    FORTID,
    FREMTID
}
