package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.vedtak.VedtakPersonYtelsesperiode
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators

fun beregnUtbetalingsperioder(personer: List<VedtakPersonYtelsesperiode>): Map<String, LocalDateTimeline<Int>> {
    return personer.groupBy(
            { it.type.klassifisering },
            { personTilTimeline(it) }
    ).mapValues { it.value.reduce(::reducer) }
}

private fun personTilTimeline(it: VedtakPersonYtelsesperiode) =
        LocalDateTimeline(listOf(LocalDateSegment<Int>(it.stønadFom, it.stønadTom, it.beløp)))

private fun reducer(sammenlagtTidslinje: LocalDateTimeline<Int>, tidslinje: LocalDateTimeline<Int>): LocalDateTimeline<Int> {
    return sammenlagtTidslinje.combine(tidslinje,
                                       StandardCombinators::sum,
                                       LocalDateTimeline.JoinStyle.CROSS_JOIN)
}



