package no.nav.familie.ba.sak.kjerne.tidslinje.tid

enum class Uendelighet {
    INGEN,
    FORTID,
    FREMTID
}

interface Tidsenhet
class Dag : Tidsenhet
class Måned : Tidsenhet

abstract class Tidspunkt<T : Tidsenhet> internal constructor(
    internal open val uendelighet: Uendelighet
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

fun <T : Tidsenhet> størsteAv(t1: Tidspunkt<T>, t2: Tidspunkt<T>): Tidspunkt<T> =
    if (t1.erUendeligLengeTil() && t2.erEndelig() && t1.somEndelig() <= t2) {
        t2.neste().somUendeligLengeTil()
    } else if (t2.erUendeligLengeTil() && t1.erEndelig() && t2.somEndelig() <= t1) {
        t1.neste().somUendeligLengeTil()
    } else if (t1.erUendeligLengeTil() || t2.erUendeligLengeTil()) {
        maxOf(t1.somEndelig(), t2.somEndelig()).somUendeligLengeTil()
    } else {
        maxOf(t1, t2)
    }

fun <T : Tidsenhet> minsteAv(t1: Tidspunkt<T>, t2: Tidspunkt<T>): Tidspunkt<T> =
    if (t1.erUendeligLengeSiden() && t2.erEndelig() && t1.somEndelig() >= t2) {
        t2.forrige().somUendeligLengeSiden()
    } else if (t2.erUendeligLengeSiden() && t1.erEndelig() && t2.somEndelig() >= t1) {
        t1.forrige().somUendeligLengeSiden()
    } else if (t1.erUendeligLengeSiden() || t2.erUendeligLengeSiden()) {
        minOf(t1.somEndelig(), t2.somEndelig()).somUendeligLengeSiden()
    } else {
        minOf(t1, t2)
    }

fun <T : Tidsenhet> Iterable<Tidspunkt<T>>.størsteEllerNull() =
    this.reduceOrNull { acc, neste -> størsteAv(acc, neste) }

fun <T : Tidsenhet> Iterable<Tidspunkt<T>>.minsteEllerNull() =
    this.reduceOrNull { acc, neste -> minsteAv(acc, neste) }
