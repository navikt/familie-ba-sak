package no.nav.familie.ba.sak.kjerne.tidslinje

import java.time.LocalDate

data class Tidsrom(
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

        val NULL = Tidspunkt.med(LocalDate.now())..Tidspunkt.med(LocalDate.now().minusDays(2))
    }
}

operator fun Tidspunkt.rangeTo(tilOgMed: Tidspunkt): Tidsrom =
    Tidsrom(this, tilOgMed)
