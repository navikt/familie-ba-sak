package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.TidspunktClosedRange

data class Periode<T>(
    val fraOgMed: Tidspunkt,
    val tilOgMed: Tidspunkt,
    val innhold: T? = null,
) {
    constructor(tidsrom: TidspunktClosedRange, innhold: T?) : this(tidsrom.start, tidsrom.endInclusive, innhold)

    override fun toString(): String = "$fraOgMed - $tilOgMed: $innhold"
}
