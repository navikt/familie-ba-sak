package no.nav.familie.ba.sak.kjerne.tidslinje.tid

data class TidspunktClosedRange<T : Tidsenhet>(
    override val start: Tidspunkt<T>,
    override val endInclusive: Tidspunkt<T>
) : Iterable<Tidspunkt<T>>,
    ClosedRange<Tidspunkt<T>> {

    override fun iterator(): Iterator<Tidspunkt<T>> =
        TidspunktIterator(start, endInclusive)

    override fun toString(): String =
        "$start - $endInclusive"

    companion object {
        private class TidspunktIterator<T : Tidsenhet>(
            val startTidspunkt: Tidspunkt<T>,
            val tilOgMedTidspunkt: Tidspunkt<T>
        ) : Iterator<Tidspunkt<T>> {

            private var gjeldendeTidspunkt = startTidspunkt.somEndelig()

            override fun hasNext() =
                gjeldendeTidspunkt.neste() <= tilOgMedTidspunkt.neste().somEndelig()

            override fun next(): Tidspunkt<T> {
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

operator fun <T : Tidsenhet> Tidspunkt<T>.rangeTo(tilOgMed: Tidspunkt<T>): TidspunktClosedRange<T> =
    TidspunktClosedRange(this, tilOgMed)
