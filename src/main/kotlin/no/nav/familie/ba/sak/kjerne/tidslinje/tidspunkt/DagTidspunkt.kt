package no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt

import java.time.LocalDate

data class DagTidspunkt internal constructor(
    internal val dato: LocalDate,
    override val uendelighet: Uendelighet,
) : Tidspunkt<Dag>(uendelighet) {

    fun tilLocalDateEllerNull(): LocalDate? {
        return if (uendelighet != Uendelighet.INGEN) {
            null
        } else {
            dato
        }
    }

    fun tilLocalDate(): LocalDate {
        return tilLocalDateEllerNull() ?: throw IllegalStateException("Tidspunkt er uendelig")
    }

    override fun flytt(tidsenheter: Long): DagTidspunkt {
        return this.copy(dato = dato.plusDays(tidsenheter), uendelighet)
    }

    override fun medUendelighet(uendelighet: Uendelighet): DagTidspunkt =
        copy(uendelighet = uendelighet)

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
        return dato.compareTo((tidspunkt as DagTidspunkt).dato)
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is DagTidspunkt -> compareTo(other) == 0
            else -> super.equals(other)
        }
    }

    companion object {
        fun nå() = DagTidspunkt(LocalDate.now(), Uendelighet.INGEN)
        fun uendeligLengeSiden(dato: LocalDate = LocalDate.now()) = DagTidspunkt(dato, uendelighet = Uendelighet.FORTID)
        fun uendeligLengeTil(dato: LocalDate = LocalDate.now()) = DagTidspunkt(dato, uendelighet = Uendelighet.FREMTID)
        fun med(dato: LocalDate) = DagTidspunkt(dato, Uendelighet.INGEN)

        internal fun LocalDate?.tilTidspunktEllerTidligereEnn(tidspunkt: LocalDate?) =
            tilTidspunktEllerUendelig(tidspunkt ?: LocalDate.now(), Uendelighet.FORTID)

        internal fun LocalDate?.tilTidspunktEllerSenereEnn(tidspunkt: LocalDate?) =
            tilTidspunktEllerUendelig(tidspunkt ?: LocalDate.now(), Uendelighet.FREMTID)

        private fun LocalDate?.tilTidspunktEllerUendelig(default: LocalDate?, uendelighet: Uendelighet) =
            this?.let { DagTidspunkt(it, Uendelighet.INGEN) } ?: DagTidspunkt(
                default ?: LocalDate.now(),
                uendelighet,
            )
    }
}
