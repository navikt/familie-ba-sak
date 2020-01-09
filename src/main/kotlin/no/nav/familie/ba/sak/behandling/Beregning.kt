package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtakBarn
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import org.springframework.stereotype.Service

val ORDINÆR_BARNETRYGD = 1054
val SMÅBARNSTILLEGG = 600

@Service
class Beregning {
    fun beregnUtbetalingsperioder(barna: List<BehandlingVedtakBarn>): LocalDateTimeline<Int> {
        val tidslinjer = barna.map {
            val segmenter = mutableListOf<LocalDateSegment<Int>>()
            if (it.stønadFom.isBefore(it.barn.fødselsdato?.plusYears(6))) {
                segmenter.add(LocalDateSegment(
                        it.stønadFom,
                        it.barn.fødselsdato?.plusYears(6),
                        ORDINÆR_BARNETRYGD + SMÅBARNSTILLEGG
                ))
            }
            segmenter.add(LocalDateSegment(
                    it.barn.fødselsdato?.plusYears(6)?.plusDays(1),
                    it.stønadTom,
                    ORDINÆR_BARNETRYGD
            ))

            LocalDateTimeline(segmenter)
        }

        val fullstendigTidslinje = tidslinjer.reduce(
            fun (sammenlagtTidslinje: LocalDateTimeline<Int>, tidslinje: LocalDateTimeline<Int>): LocalDateTimeline<Int> {
                return sammenlagtTidslinje.combine(tidslinje, StandardCombinators::sum, LocalDateTimeline.JoinStyle.CROSS_JOIN)
            }
        )

        return fullstendigTidslinje
    }
}