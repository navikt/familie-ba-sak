package no.nav.familie.ba.sak.kjerne.tidslinje.tid

import no.nav.familie.ba.sak.common.toYearMonth
import java.time.LocalDate
import java.time.YearMonth

data class MånedTidspunkt(
    private val måned: YearMonth,
    override val uendelighet: Uendelighet
) : Tidspunkt {
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

    override fun tilDagIMåned(dag: Int): DagTidspunkt =
        DagTidspunkt(måned.atDay(dag), this.uendelighet)

    override fun tilDag(månedTilDagMapper: (YearMonth) -> LocalDate): DagTidspunkt =
        DagTidspunkt(månedTilDagMapper(måned), uendelighet = uendelighet)

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
