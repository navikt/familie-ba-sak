package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsrom

data class Periode<T>(
    val fom: Tidspunkt,
    val tom: Tidspunkt,
    val innhold: T? = null,
) {
    constructor(tidsrom: Tidsrom, innhold: T?) : this(tidsrom.start, tidsrom.endInclusive, innhold)

    override fun toString(): String = "$fom - $tom: $innhold"
}
