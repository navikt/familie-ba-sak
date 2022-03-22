package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.TidspunktClosedRange

data class Periode<I, T : Tidsenhet>(
    val fraOgMed: Tidspunkt<T>,
    val tilOgMed: Tidspunkt<T>,
    val innhold: I? = null,
) {
    constructor(tidsrom: TidspunktClosedRange<T>, innhold: I?) : this(tidsrom.start, tidsrom.endInclusive, innhold)

    override fun toString(): String = "$fraOgMed - $tilOgMed: $innhold"
}
