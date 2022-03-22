package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.TidspunktClosedRange

data class Periode<DATA, T : Tidsenhet>(
    val fraOgMed: Tidspunkt<T>,
    val tilOgMed: Tidspunkt<T>,
    val innhold: DATA? = null,
) {
    constructor(tidsrom: TidspunktClosedRange<T>, innhold: DATA?) : this(tidsrom.start, tidsrom.endInclusive, innhold)

    override fun toString(): String = "$fraOgMed - $tilOgMed: $innhold"
}
