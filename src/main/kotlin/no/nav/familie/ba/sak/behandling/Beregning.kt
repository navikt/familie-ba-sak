package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtakBarn
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import org.springframework.stereotype.Service

@Service
class Beregning {
    fun beregnUtbetalingsperioder(barna: List<BehandlingVedtakBarn>): LocalDateTimeline<Int> {
        val tidslinjer = barna.map {
            val segmenter = mutableListOf<LocalDateSegment<Int>>()

            segmenter.add(LocalDateSegment(
                    it.stønadFom,
                    it.stønadTom,
                    it.beløp
            ))

            LocalDateTimeline(segmenter)
        }

        return tidslinjer.reduce(
            fun (sammenlagtTidslinje: LocalDateTimeline<Int>, tidslinje: LocalDateTimeline<Int>): LocalDateTimeline<Int> {
                return sammenlagtTidslinje.combine(tidslinje, StandardCombinators::sum, LocalDateTimeline.JoinStyle.CROSS_JOIN)
            }
        )
    }
}