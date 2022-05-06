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

    override fun sammenliknMed(tidspunkt: Tidspunkt<Måned>): Int {
        return måned.compareTo(tidspunkt.tilYearMonth())
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is MånedTidspunkt -> compareTo(other) == 0
            else -> super.equals(other)
        }
    }

    companion object {
        fun nå() = MånedTidspunkt(YearMonth.now(), Uendelighet.INGEN)
        internal fun YearMonth?.tilTidspunktEllerUendeligLengeSiden(default: () -> YearMonth?) =
            this.tilTidspunktEllerUendelig(default, Uendelighet.FORTID)

        internal fun YearMonth?.tilTidspunktEllerUendeligLengeTil(default: () -> YearMonth?) =
            this.tilTidspunktEllerUendelig(default, Uendelighet.FREMTID)

        private fun YearMonth?.tilTidspunktEllerUendelig(default: () -> YearMonth?, uendelighet: Uendelighet) =
            this?.let { MånedTidspunkt(it, Uendelighet.INGEN) } ?: MånedTidspunkt(
                default() ?: YearMonth.now(),
                uendelighet
            )

        internal fun YearMonth?.tilTidspunktEllerUendeligLengeSiden() =
            this.tilTidspunktEllerUendelig({ PRAKTISK_TIDLIGSTE_DAG.toYearMonth() }, Uendelighet.FORTID)

        internal fun YearMonth?.tilTidspunktEllerUendeligLengeTil() =
            this.tilTidspunktEllerUendelig({ PRAKTISK_SENESTE_DAG.toYearMonth() }, Uendelighet.FREMTID)

        fun månedForUendeligLengeSiden(måned: YearMonth = YearMonth.now()) =
            MånedTidspunkt(måned, uendelighet = Uendelighet.FORTID)

        fun månedOmUendeligLenge(måned: YearMonth = YearMonth.now()) =
            MånedTidspunkt(måned, uendelighet = Uendelighet.FREMTID)
    }
}
