package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.TidspunktClosedRange
import java.time.temporal.Temporal

data class Periode<T, TID : Temporal>(
    val fraOgMed: Tidspunkt<TID>,
    val tilOgMed: Tidspunkt<TID>,
    val innhold: T? = null,
) {
    constructor(tidsrom: TidspunktClosedRange<TID>, innhold: T?) : this(tidsrom.start, tidsrom.endInclusive, innhold)

    override fun toString(): String = "$fraOgMed - $tilOgMed: $innhold"
}
