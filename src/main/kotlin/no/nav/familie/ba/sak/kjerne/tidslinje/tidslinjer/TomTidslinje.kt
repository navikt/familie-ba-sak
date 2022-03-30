package no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt

class TomTidslinje<I, T : Tidsenhet>(
    val nå: Tidspunkt<T>
) : Tidslinje<I, T>() {
    override fun fraOgMed(): Tidspunkt<T> = nå.somUendeligLengeSiden()
    override fun tilOgMed(): Tidspunkt<T> = nå.somUendeligLengeTil()

    override fun lagPerioder(): Collection<Periode<I, T>> = emptyList()
}
