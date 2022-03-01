package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import java.time.YearMonth

data class EndringstidspunktData(
    val aktør: Aktør,
    val kalkulertBeløp: Int
)

fun førsteEndringstidspunkt(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
): YearMonth? {
    val forrigeAndelerTidslinje = LocalDateTimeline(
        forrigeAndelerTilkjentYtelse.map {
            LocalDateSegment(
                it.stønadFom.førsteDagIInneværendeMåned(),
                it.stønadTom.sisteDagIInneværendeMåned(),
                EndringstidspunktData(
                    aktør = it.aktør,
                    kalkulertBeløp = it.kalkulertUtbetalingsbeløp
                )
            )
        }
    )
    val andelerTidslinje = LocalDateTimeline(
        andelerTilkjentYtelse.map {
            LocalDateSegment(
                it.stønadFom.førsteDagIInneværendeMåned(),
                it.stønadTom.sisteDagIInneværendeMåned(),
                EndringstidspunktData(
                    aktør = it.aktør,
                    kalkulertBeløp = it.kalkulertUtbetalingsbeløp
                )
            )
        }
    )

    val a = andelerTidslinje.combine(
        forrigeAndelerTidslinje,
        StandardCombinators::bothValues,
        LocalDateTimeline.JoinStyle.CROSS_JOIN
    ) as LocalDateTimeline<List<EndringstidspunktData>>

    val b = a.toSegments()
        .filter {
            it.value.groupBy { v -> v.aktør.aktørId }.any { a ->
                if (a.value.size == 1) true
                else {
                    val nySum = a.value[0].kalkulertBeløp
                    val forrigeSumD = a.value[1].kalkulertBeløp
                    (nySum - forrigeSumD) != 0
                }
            }
        }

    return b.minOfOrNull { it.fom }?.toYearMonth()
}