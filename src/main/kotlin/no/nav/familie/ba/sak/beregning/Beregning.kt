package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators

fun beregnUtbetalingsperioder(andelerTilkjentYtelse: List<AndelTilkjentYtelse>): Map<String, LocalDateTimeline<Int>> {
    return andelerTilkjentYtelse.groupBy(
            { it.type.klassifisering },
            { personTilTimeline(it) }
    ).mapValues { it.value.reduce(::reducer) }
}

fun beregnUtbetalingsperioderUtenKlassifisering(andelerTilkjentYtelse: Set<AndelTilkjentYtelse>): LocalDateTimeline<Int> {
    return andelerTilkjentYtelse
            .map { personTilTimeline(it) }
            .reduce (::reducer)
}

private fun personTilTimeline(it: AndelTilkjentYtelse) =
        LocalDateTimeline(listOf(LocalDateSegment(it.stønadFom, it.stønadTom, it.beløp)))

private fun reducer(sammenlagtTidslinje: LocalDateTimeline<Int>, tidslinje: LocalDateTimeline<Int>): LocalDateTimeline<Int> {
    return sammenlagtTidslinje.combine(tidslinje,
                                       StandardCombinators::sum,
                                       LocalDateTimeline.JoinStyle.CROSS_JOIN)
}



