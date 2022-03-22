package no.nav.familie.ba.sak.kjerne.tidslinje.tid

import no.nav.familie.ba.sak.common.sisteDagIMåned
import java.time.LocalDate
import java.time.YearMonth

data class DagTidspunkt(
    private val dato: LocalDate,
    override val uendelighet: Uendelighet
) : Tidspunkt {

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

    override fun tilDagIMåned(dag: Int): DagTidspunkt {
        return this.copy(dato = dato.withDayOfMonth(dag))
    }

    override fun tilDag(månedTilDagMapper: (YearMonth) -> LocalDate): DagTidspunkt = this

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

    override fun flytt(tidsenheter: Long): Tidspunkt {
        return this.copy(dato = dato.plusDays(tidsenheter))
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
    override fun toString(): String {
        return when (uendelighet) {
            Uendelighet.FORTID -> "<--"
            else -> ""
        } + dato + when (uendelighet) {
            Uendelighet.FREMTID -> "-->"
            else -> ""
        }
    }
}
