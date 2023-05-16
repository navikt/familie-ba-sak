package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom.TidspunktClosedRange

data class Periode<I, T : Tidsenhet>(
    val fraOgMed: Tidspunkt<T>,
    val tilOgMed: Tidspunkt<T>,
    val innhold: I? = null,
) {
    constructor(tidsrom: TidspunktClosedRange<T>, innhold: I?) : this(tidsrom.start, tidsrom.endInclusive, innhold)

    override fun toString(): String = "$fraOgMed - $tilOgMed: $innhold"
}

fun <I, T : Tidsenhet> Tidspunkt<T>.tilPeriodeMedInnhold(innhold: I?) = Periode(this, this, innhold)

fun <I, T : Tidsenhet> Tidspunkt<T>.tilPeriodeUtenInnhold() = tilPeriodeMedInnhold(null as I)
