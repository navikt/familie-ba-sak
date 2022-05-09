package no.nav.familie.ba.sak.kjerne.tidslinje.tid

import no.nav.familie.ba.sak.common.sisteDagIMåned
import java.time.LocalDate
import java.time.YearMonth

data class DagTidspunkt internal constructor(
    private val dato: LocalDate,
    val uendelighet: Uendelighet
) : Tidspunkt<Dag>(uendelighet) {

    init {
        if (dato < PRAKTISK_TIDLIGSTE_DAG)
            throw IllegalArgumentException("Kan ikke håndtere så tidlig tidspunkt. Bruk uendeligLengeSiden()")
        else if (dato > PRAKTISK_SENESTE_DAG)
            throw IllegalArgumentException("Kan ikke håndtere så sent tidspunkt. Bruk uendeligLengeTil()")
    }

    override fun tilFørsteDagIMåneden(): DagTidspunkt {
        return this.copy(dato = dato.withDayOfMonth(1))
    }

    override fun tilSisteDagIMåneden(): DagTidspunkt {
        return this.copy(dato = dato.sisteDagIMåned())
    }

    override fun tilInneværendeMåned(): MånedTidspunkt {
        return MånedTidspunkt(dagTilMånedKonverterer(this.dato), uendelighet)
    }

    override fun tilLocalDateEllerNull(): LocalDate? {
        return if (uendelighet != Uendelighet.INGEN)
            null
        else
            dato
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

    override fun flytt(tidsenheter: Long): DagTidspunkt {
        return this.copy(dato = dato.plusDays(tidsenheter))
    }

    override fun somEndelig(): DagTidspunkt {
        return copy(uendelighet = Uendelighet.INGEN)
    }

    override fun somUendeligLengeSiden(): DagTidspunkt {
        return copy(uendelighet = Uendelighet.FORTID)
    }

    override fun somUendeligLengeTil(): DagTidspunkt {
        return copy(uendelighet = Uendelighet.FREMTID)
    }

    override fun somFraOgMed(): DagTidspunkt {
        return if (uendelighet == Uendelighet.FREMTID)
            somEndelig()
        else
            this
    }

    override fun somTilOgMed(): DagTidspunkt {
        return if (uendelighet == Uendelighet.FORTID)
            somEndelig()
        else
            this
    }

    override fun toString(): String {
        return when (uendelighet) {
            Uendelighet.FORTID -> "<--"
            else -> ""
        } + dato + when (uendelighet) {
            Uendelighet.FREMTID -> "-->"
            else -> ""
        }
    }

    override fun sammenliknMed(tidspunkt: Tidspunkt<Dag>): Int {
        return dato.compareTo(tidspunkt.tilLocalDate())
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is DagTidspunkt -> compareTo(other) == 0
            else -> super.equals(other)
        }
    }

    companion object {
        fun nå() = DagTidspunkt(LocalDate.now(), Uendelighet.INGEN)

        internal fun LocalDate?.tilTidspunktEllerTidligereEnn(tidspunkt: LocalDate?) =
            tilTidspunktEllerUendelig(tidspunkt ?: LocalDate.now(), Uendelighet.FORTID)

        internal fun LocalDate?.tilTidspunktEllerSenereEnn(tidspunkt: LocalDate?) =
            tilTidspunktEllerUendelig(tidspunkt ?: LocalDate.now(), Uendelighet.FREMTID)

        internal fun LocalDate?.tilTidspunktEllerUendeligLengeSiden() =
            this.tilTidspunktEllerUendelig(PRAKTISK_TIDLIGSTE_DAG, Uendelighet.FORTID)

        internal fun LocalDate?.tilTidspunktEllerUendeligLengeTil() =
            this.tilTidspunktEllerUendelig(PRAKTISK_SENESTE_DAG, Uendelighet.FREMTID)

        private fun LocalDate?.tilTidspunktEllerUendelig(default: LocalDate?, uendelighet: Uendelighet) =
            this?.let { DagTidspunkt(it, Uendelighet.INGEN) } ?: DagTidspunkt(
                default ?: LocalDate.now(),
                uendelighet
            )

        fun dagForUendeligLengeSiden(dato: LocalDate = LocalDate.now()) =
            DagTidspunkt(dato, uendelighet = Uendelighet.FORTID)

        fun dagMedUendeligLengeTil(dato: LocalDate = LocalDate.now()) =
            DagTidspunkt(dato, uendelighet = Uendelighet.FREMTID)
    }
}
