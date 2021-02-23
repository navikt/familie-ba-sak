package no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import java.time.LocalDate

data class Opphørsperiode(
        override val periodeFom: LocalDate,
        override val periodeTom: LocalDate,
        override val vedtaksperiodetype: Vedtaksperiodetype = Vedtaksperiodetype.OPPHØR
) : Vedtaksperiode

fun finnOpphørsperioder(forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
                        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
                        personopplysningGrunnlag: PersonopplysningGrunnlag): List<Opphørsperiode> {
    val utbetalingsperioder = mapTilUtbetalingsperioder(personopplysningGrunnlag, andelerTilkjentYtelse)

    return if (forrigeAndelerTilkjentYtelse.isEmpty()) {
        listOf(
                finnOpphørsperioderMellomUtbetalingsperioder(utbetalingsperioder),
                finnOpphørsperiodeEtterSisteUtbetalingsperiode(utbetalingsperioder)
        ).flatten()
    } else {
        emptyList()
    }
}

private fun finnOpphørsperioderMellomUtbetalingsperioder(utbetalingsperioder: List<Utbetalingsperiode>): List<Opphørsperiode> {
    val helYtelseTidslinje = LocalDateTimeline(
            listOf(LocalDateSegment(
                    utbetalingsperioder.minOf { it.periodeFom },
                    utbetalingsperioder.maxOf { it.periodeTom },
                    1
            )))

    val andelerTidslinje = LocalDateTimeline(utbetalingsperioder.map {
        LocalDateSegment(
                it.periodeFom,
                it.periodeTom,
                it
        )
    })

    val segmenterFjernet = helYtelseTidslinje.disjoint(andelerTidslinje)

    return segmenterFjernet.toList().map {
        Opphørsperiode(
                periodeFom = it.fom,
                periodeTom = it.tom,
                vedtaksperiodetype = Vedtaksperiodetype.OPPHØR
        )
    }
}

private fun finnOpphørsperiodeEtterSisteUtbetalingsperiode(utbetalingsperioder: List<Utbetalingsperiode>): List<Opphørsperiode> {
    val sisteUtbetalingsperiodeTom = utbetalingsperioder.maxOf { it.periodeTom }.toYearMonth().nesteMåned()
    val nesteMåned = inneværendeMåned().nesteMåned()

    return if (sisteUtbetalingsperiodeTom.isBefore(nesteMåned)) {
        listOf(Opphørsperiode(
                periodeFom = sisteUtbetalingsperiodeTom.førsteDagIInneværendeMåned(),
                periodeTom = nesteMåned.sisteDagIInneværendeMåned(),
                vedtaksperiodetype = Vedtaksperiodetype.OPPHØR
        ))
    } else {
        emptyList()
    }
}