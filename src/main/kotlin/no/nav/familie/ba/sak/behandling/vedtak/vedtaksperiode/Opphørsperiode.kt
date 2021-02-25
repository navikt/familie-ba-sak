package no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.common.TIDENES_ENDE
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

fun mapTilOpphørsperioder(forrigePersonopplysningGrunnlag: PersonopplysningGrunnlag? = null,
                          forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
                          personopplysningGrunnlag: PersonopplysningGrunnlag,
                          andelerTilkjentYtelse: List<AndelTilkjentYtelse>): List<Opphørsperiode> {
    val forrigeUtbetalingsperioder = if (forrigePersonopplysningGrunnlag != null) mapTilUtbetalingsperioder(
            forrigePersonopplysningGrunnlag,
            forrigeAndelerTilkjentYtelse) else emptyList()
    val utbetalingsperioder = mapTilUtbetalingsperioder(personopplysningGrunnlag, andelerTilkjentYtelse)

    return if (forrigeUtbetalingsperioder.isNotEmpty() && utbetalingsperioder.isEmpty()) {
        listOf(Opphørsperiode(
                periodeFom = forrigeUtbetalingsperioder.minOf { it.periodeFom },
                periodeTom = forrigeUtbetalingsperioder.maxOf { it.periodeTom }
        ))
    } else if (utbetalingsperioder.isEmpty()) {
        emptyList()
    } else {
        listOf(
                finnOpphørsperioderPåGrunnAvReduksjonIRevurdering(forrigeUtbetalingsperioder = forrigeUtbetalingsperioder,
                                                                  utbetalingsperioder = utbetalingsperioder),
                finnOpphørsperioderMellomUtbetalingsperioder(utbetalingsperioder),
                finnOpphørsperiodeEtterSisteUtbetalingsperiode(utbetalingsperioder)
        ).flatten()
    }
}

private fun finnOpphørsperioderMellomUtbetalingsperioder(utbetalingsperioder: List<Utbetalingsperiode>): List<Opphørsperiode> {
    val helYtelseTidslinje = LocalDateTimeline(
            listOf(LocalDateSegment(
                    utbetalingsperioder.minOf { it.periodeFom },
                    utbetalingsperioder.maxOf { it.periodeTom },
                    null
            )))

    return utledSegmenterFjernet(utbetalingsperioder, helYtelseTidslinje)
}

private fun finnOpphørsperiodeEtterSisteUtbetalingsperiode(utbetalingsperioder: List<Utbetalingsperiode>): List<Opphørsperiode> {
    val sisteUtbetalingsperiodeTom = utbetalingsperioder.maxOf { it.periodeTom }.toYearMonth()
    val nesteMåned = inneværendeMåned().nesteMåned()

    return if (sisteUtbetalingsperiodeTom.isBefore(nesteMåned)) {
        listOf(Opphørsperiode(
                periodeFom = sisteUtbetalingsperiodeTom.nesteMåned().førsteDagIInneværendeMåned(),
                periodeTom = TIDENES_ENDE,
                vedtaksperiodetype = Vedtaksperiodetype.OPPHØR
        ))
    } else {
        emptyList()
    }
}

private fun finnOpphørsperioderPåGrunnAvReduksjonIRevurdering(forrigeUtbetalingsperioder: List<Utbetalingsperiode>,
                                                              utbetalingsperioder: List<Utbetalingsperiode>): List<Opphørsperiode> {
    val forrigeUtbetalingstidslinje = LocalDateTimeline(forrigeUtbetalingsperioder.map {
        LocalDateSegment(
                it.periodeFom,
                it.periodeTom,
                null
        )
    })

    return utledSegmenterFjernet(utbetalingsperioder, forrigeUtbetalingstidslinje)
}

private fun utledSegmenterFjernet(utbetalingsperioder: List<Utbetalingsperiode>,
                                  sammenligningstidslinje: LocalDateTimeline<Nothing?>): List<Opphørsperiode> {
    val utbetalingstidslinje = LocalDateTimeline(utbetalingsperioder.map {
        LocalDateSegment(
                it.periodeFom,
                it.periodeTom,
                it
        )
    })

    val segmenterFjernet = sammenligningstidslinje.disjoint(utbetalingstidslinje)

    return segmenterFjernet.toList().map {
        Opphørsperiode(
                periodeFom = it.fom,
                periodeTom = it.tom,
                vedtaksperiodetype = Vedtaksperiodetype.OPPHØR
        )
    }
}