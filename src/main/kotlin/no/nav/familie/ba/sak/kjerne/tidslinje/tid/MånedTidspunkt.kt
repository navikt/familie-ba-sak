package no.nav.familie.ba.sak.kjerne.tidslinje.tid

import no.nav.familie.ba.sak.common.toYearMonth
import java.time.LocalDate
import java.time.YearMonth

data class MånedTidspunkt internal constructor(
    private val måned: YearMonth,
    private val uendelighet: Uendelighet
) : Tidspunkt<Måned>(uendelighet) {
    init {
        if (måned < PRAKTISK_TIDLIGSTE_DAG.toYearMonth())
            throw IllegalArgumentException("Tidspunktet er for lenge siden. Bruk uendeligLengeSiden()")
        else if (måned > PRAKTISK_SENESTE_DAG.toYearMonth())
            throw IllegalArgumentException("Tidspunktet er for lenge til. Bruk uendeligLengeTil()")
    }

    override fun tilFørsteDagIMåneden(): DagTidspunkt =
        DagTidspunkt(måned.atDay(1), this.uendelighet)

    override fun tilSisteDagIMåneden(): DagTidspunkt =
        DagTidspunkt(måned.atEndOfMonth(), this.uendelighet)

    override fun tilInneværendeMåned() = this

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

    override fun flytt(tidsenheter: Long) = copy(måned = måned.plusMonths(tidsenheter))

    override fun somEndelig(): MånedTidspunkt = this.copy(uendelighet = Uendelighet.INGEN)

    override fun toString(): String {
        return when (uendelighet) {
            Uendelighet.FORTID -> "<--"
            else -> ""
        } + måned + when (uendelighet) {
            Uendelighet.FREMTID -> "-->"
            else -> ""
        }
    }

    override fun somUendeligLengeSiden(): MånedTidspunkt = this.copy(uendelighet = Uendelighet.FORTID)
    override fun somUendeligLengeTil(): MånedTidspunkt = this.copy(uendelighet = Uendelighet.FREMTID)
    override fun somFraOgMed(): MånedTidspunkt {
        return if (uendelighet == Uendelighet.FREMTID) {
            somEndelig()
        } else
            this
    }

    override fun somTilOgMed(): MånedTidspunkt {
        return if (uendelighet == Uendelighet.FORTID) {
            somEndelig()
        } else
            this
    }

    override fun somFraOgMed(dato: LocalDate): MånedTidspunkt {
        return if (dato < PRAKTISK_TIDLIGSTE_DAG)
            MånedTidspunkt(PRAKTISK_TIDLIGSTE_DAG.toYearMonth(), Uendelighet.FORTID)
        else
            MånedTidspunkt(dato.toYearMonth(), Uendelighet.INGEN)
    }

    override fun somTilOgMed(dato: LocalDate): MånedTidspunkt {
        return if (dato > PRAKTISK_SENESTE_DAG)
            MånedTidspunkt(PRAKTISK_SENESTE_DAG.toYearMonth(), Uendelighet.FREMTID)
        else
            MånedTidspunkt(dato.toYearMonth(), Uendelighet.INGEN).somTilOgMed()
    }

    override fun sammenliknMed(tidspunkt: Tidspunkt<Måned>): Int {
        return måned.compareTo(tidspunkt.tilYearMonth())
    }

    companion object {
        fun nå() = MånedTidspunkt(YearMonth.now(), Uendelighet.INGEN)
    }
}
