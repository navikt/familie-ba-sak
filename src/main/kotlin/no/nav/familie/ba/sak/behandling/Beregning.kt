package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.vedtak.Ytelsetype
import no.nav.familie.ba.sak.behandling.domene.vedtak.VedtakPerson
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import org.springframework.stereotype.Service

@Service
class Beregning {

    fun <T> beregnUtbetalingsperioder(personer: List<VedtakPerson>, groupMapper: (Ytelsetype) -> T): Map<T, LocalDateTimeline<Int>> {
        return personer.groupBy(
                { groupMapper(it.type) },
                { personTilTimeline(it) }
        ).mapValues { it.value.reduce(::reducer) }
    }

    private fun personTilTimeline(it: VedtakPerson) =
            LocalDateTimeline(listOf(LocalDateSegment<Int>(it.stønadFom, it.stønadTom, it.beløp)))

    private fun reducer(sammenlagtTidslinje: LocalDateTimeline<Int>, tidslinje: LocalDateTimeline<Int>): LocalDateTimeline<Int> {
        return sammenlagtTidslinje.combine(tidslinje,
                                           StandardCombinators::sum,
                                           LocalDateTimeline.JoinStyle.CROSS_JOIN)
    }
}
