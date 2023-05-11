package no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt

enum class Uendelighet {
    INGEN,
    FORTID,
    FREMTID,
}

interface Tidsenhet
class Dag : Tidsenhet
class Måned : Tidsenhet

abstract class Tidspunkt<T : Tidsenhet> internal constructor(
    internal open val uendelighet: Uendelighet,
) : Comparable<Tidspunkt<T>> {
    abstract fun flytt(tidsenheter: Long): Tidspunkt<T>
    internal abstract fun medUendelighet(uendelighet: Uendelighet): Tidspunkt<T>

    // Betrakter to uendeligheter som like, selv underliggende tidspunkt kan være forskjellig
    override fun compareTo(other: Tidspunkt<T>) =
        if (this.uendelighet == Uendelighet.FORTID && other.uendelighet == Uendelighet.FORTID) {
            0
        } else if (this.uendelighet == Uendelighet.FREMTID && other.uendelighet == Uendelighet.FREMTID) {
            0
        } else if (this.uendelighet == Uendelighet.FORTID && other.uendelighet != Uendelighet.FORTID) {
            -1
        } else if (this.uendelighet == Uendelighet.FREMTID && other.uendelighet != Uendelighet.FREMTID) {
            1
        } else if (this.uendelighet != Uendelighet.FORTID && other.uendelighet == Uendelighet.FORTID) {
            1
        } else if (this.uendelighet != Uendelighet.FREMTID && other.uendelighet == Uendelighet.FREMTID) {
            -1
        } else {
            sammenliknMed(other)
        }

    protected abstract fun sammenliknMed(tidspunkt: Tidspunkt<T>): Int

    override fun equals(other: Any?): Boolean {
        return if (other is Tidspunkt<*>) {
            this.uendelighet != Uendelighet.INGEN && this.uendelighet == other.uendelighet
        } else {
            super.equals(other)
        }
    }
}
