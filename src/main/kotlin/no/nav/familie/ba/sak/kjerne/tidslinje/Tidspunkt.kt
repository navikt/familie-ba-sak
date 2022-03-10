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

class TidspunktProgression(
    override val start: Tidspunkt,
    override val endInclusive: Tidspunkt,
    val hopp: Long = 1
) : Iterable<Tidspunkt>,
    ClosedRange<Tidspunkt> {

    override fun iterator(): Iterator<Tidspunkt> =
        TidspunktIterator(start, endInclusive, hopp)

    infix fun step(hopp: Long) = TidspunktProgression(start, endInclusive, hopp)

    companion object {
        private class TidspunktIterator(
            startTidspunkt: Tidspunkt,
            val tilOgMedTidspunkt: Tidspunkt,
            val hopp: Long,
        ) : Iterator<Tidspunkt> {

            init {
                if (!startTidspunkt.harSammePresisjon(tilOgMedTidspunkt)) {
                    throw IllegalArgumentException("Tidspunktene har ikke samme presisjon")
                }
            }

            private var gjeldendeTidspunkt = startTidspunkt

            override fun hasNext() =
                if (hopp > 0)
                    gjeldendeTidspunkt.hopp(hopp) <= tilOgMedTidspunkt.neste()
                else if (hopp < 0)
                    gjeldendeTidspunkt.hopp(hopp) >= tilOgMedTidspunkt.forrige()
                else
                    throw IllegalStateException("Hopp kan ikke være 0")

            override fun next(): Tidspunkt {
                val next = gjeldendeTidspunkt
                gjeldendeTidspunkt = gjeldendeTidspunkt.hopp(hopp)
                return next
            }
        }
    }
}

class FleksibelTidspunktIterator(
    startTidspunkt: Tidspunkt,
    val tilOgMedTidspunkt: Tidspunkt
) : Iterator<Tidspunkt> {

    private var gjeldendeTidspunkt = startTidspunkt

    override fun hasNext() =
        gjeldendeTidspunkt.neste() <= tilOgMedTidspunkt.neste()

    override fun next(): Tidspunkt {
        val neste = gjeldendeTidspunkt.neste()
        val nesteErNyMåned = gjeldendeTidspunkt.tilMåned() < neste.tilMåned()

        if (nesteErNyMåned) {
            if (neste.tilMåned() < tilOgMedTidspunkt.tilMåned())
                gjeldendeTidspunkt = neste.tilMåned()
            else if (neste.tilMåned() == tilOgMedTidspunkt.tilMåned() && tilOgMedTidspunkt.harPresisjonDag())
                gjeldendeTidspunkt = tilOgMedTidspunkt.tilFørsteDagIMåneden()
        } else
            gjeldendeTidspunkt = neste

        return gjeldendeTidspunkt
    }
}

class FleksibeltTidspunktClosedRange(
    override val start: Tidspunkt,
    override val endInclusive: Tidspunkt
) : Iterable<Tidspunkt>,
    ClosedRange<Tidspunkt> {

    override fun iterator(): Iterator<Tidspunkt> =
        FleksibelTidspunktIterator(start, endInclusive)
}

operator fun Tidspunkt.rangeTo(tilOgMed: Tidspunkt) =
    FleksibeltTidspunktClosedRange(this, tilOgMed)
