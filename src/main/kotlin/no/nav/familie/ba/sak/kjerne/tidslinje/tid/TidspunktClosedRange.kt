package no.nav.familie.ba.sak.kjerne.tidslinje.tid

data class TidspunktClosedRange(
    override val start: Tidspunkt,
    override val endInclusive: Tidspunkt
) : Iterable<Tidspunkt>,
    ClosedRange<Tidspunkt> {

    override fun iterator(): Iterator<Tidspunkt> =
        if (start.erDag() && !endInclusive.erDag())
            TidspunktIterator(start, endInclusive.tilSisteDagIMåneden())
        else if (!start.erDag() && endInclusive.erDag())
            TidspunktIterator(start.tilFørsteDagIMåneden(), endInclusive)
        else
            TidspunktIterator(start, endInclusive)

    override fun toString(): String =
        "$start - $endInclusive"

    companion object {
        private class TidspunktIterator(
            val startTidspunkt: Tidspunkt,
            val tilOgMedTidspunkt: Tidspunkt
        ) : Iterator<Tidspunkt> {

            private var gjeldendeTidspunkt = startTidspunkt.somEndelig()

            override fun hasNext() =
                gjeldendeTidspunkt.neste() <= tilOgMedTidspunkt.neste().somEndelig()

            override fun next(): Tidspunkt {
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

operator fun Tidspunkt.rangeTo(tilOgMed: Tidspunkt): TidspunktClosedRange =
    TidspunktClosedRange(this, tilOgMed)
