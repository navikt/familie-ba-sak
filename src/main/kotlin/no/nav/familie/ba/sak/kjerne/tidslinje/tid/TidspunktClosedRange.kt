package no.nav.familie.ba.sak.kjerne.tidslinje.tid

import java.time.temporal.Temporal

data class TidspunktClosedRange<TID : Temporal>(
    override val start: Tidspunkt<TID>,
    override val endInclusive: Tidspunkt<TID>
) : Iterable<Tidspunkt<TID>>,
    ClosedRange<Tidspunkt<TID>> {

    override fun iterator(): Iterator<Tidspunkt<TID>> =
        TidspunktIterator(start, endInclusive)

    override fun toString(): String =
        "$start - $endInclusive"

    companion object {
        private class TidspunktIterator<TID : Temporal>(
            val startTidspunkt: Tidspunkt<TID>,
            val tilOgMedTidspunkt: Tidspunkt<TID>
        ) : Iterator<Tidspunkt<TID>> {

            private var gjeldendeTidspunkt = startTidspunkt.somEndelig()

            override fun hasNext() =
                gjeldendeTidspunkt.neste() <= tilOgMedTidspunkt.neste().somEndelig()

            override fun next(): Tidspunkt<TID> {
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
    }
}

operator fun <TID : Temporal> Tidspunkt<TID>.rangeTo(tilOgMed: Tidspunkt<TID>): TidspunktClosedRange<TID> =
    TidspunktClosedRange(this, tilOgMed)
