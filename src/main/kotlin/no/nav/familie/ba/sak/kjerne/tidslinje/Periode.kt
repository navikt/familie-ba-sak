package no.nav.familie.ba.sak.kjerne.tidslinje

data class Periode<T>(
    val fom: Tidspunkt,
    val tom: Tidspunkt,
    val innhold: T? = null,
) {
    constructor(tidsrom: Tidsrom, innhold: T?) : this(tidsrom.start, tidsrom.endInclusive, innhold)

    override fun toString(): String = "$fom - $tom: $innhold"
}
