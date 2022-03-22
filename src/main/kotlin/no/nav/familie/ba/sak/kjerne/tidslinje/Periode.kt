package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.TidspunktClosedRange
import java.time.temporal.Temporal

data class Periode<DATA, TID : Temporal>(
    val fraOgMed: Tidspunkt<TID>,
    val tilOgMed: Tidspunkt<TID>,
    val innhold: DATA? = null,
) {
    constructor(tidsrom: TidspunktClosedRange<TID>, innhold: DATA?) : this(tidsrom.start, tidsrom.endInclusive, innhold)

    override fun toString(): String = "$fraOgMed - $tilOgMed: $innhold"
}
