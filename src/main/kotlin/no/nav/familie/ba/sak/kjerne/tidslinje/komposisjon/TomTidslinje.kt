package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet

class TomTidslinje<I, T : Tidsenhet> : Tidslinje<I, T>() {
    override fun lagPerioder(): Collection<Periode<I, T>> = emptyList()
}
