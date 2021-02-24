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

fun finnOpphørsperioder(forrigePersonopplysningGrunnlag: PersonopplysningGrunnlag? = null,
                        forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
                        personopplysningGrunnlag: PersonopplysningGrunnlag,
                        andelerTilkjentYtelse: List<AndelTilkjentYtelse>): List<Opphørsperiode> {
    val forrigeUtbetalingsperioder = if (forrigePersonopplysningGrunnlag != null) mapTilUtbetalingsperioder(
            forrigePersonopplysningGrunnlag,
            forrigeAndelerTilkjentYtelse) else emptyList()
    val utbetalingsperioder = mapTilUtbetalingsperioder(personopplysningGrunnlag, andelerTilkjentYtelse)

    return listOf(
            finnOpphørsperioderFraForrigeBehandling(forrigeUtbetalingsperioder = forrigeUtbetalingsperioder,
                                                    utbetalingsperioder = utbetalingsperioder),
            finnOpphørsperioderMellomUtbetalingsperioder(utbetalingsperioder),
            finnOpphørsperiodeEtterSisteUtbetalingsperiode(utbetalingsperioder)
    ).flatten()
}

private fun finnOpphørsperioderMellomUtbetalingsperioder(utbetalingsperioder: List<Utbetalingsperiode>): List<Opphørsperiode> {
    val helYtelseTidslinje = LocalDateTimeline(
            listOf(LocalDateSegment(
                    utbetalingsperioder.minOf { it.periodeFom },
                    utbetalingsperioder.maxOf { it.periodeTom },
                    1
            )))

    val utbetalingstidslinje = LocalDateTimeline(utbetalingsperioder.map {
        LocalDateSegment(
                it.periodeFom,
                it.periodeTom,
                it
        )
    })

    val segmenterFjernet = helYtelseTidslinje.disjoint(utbetalingstidslinje)

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

private fun finnOpphørsperioderFraForrigeBehandling(forrigeUtbetalingsperioder: List<Utbetalingsperiode>,
                                                    utbetalingsperioder: List<Utbetalingsperiode>): List<Opphørsperiode> {
    val forrigeUtbetalingstidslinje = LocalDateTimeline(forrigeUtbetalingsperioder.map {
        LocalDateSegment(
                it.periodeFom,
                it.periodeTom,
                it
        )
    })

    val utbetalingstidslinje = LocalDateTimeline(utbetalingsperioder.map {
        LocalDateSegment(
                it.periodeFom,
                it.periodeTom,
                it
        )
    })

    val segmenterFjernet = forrigeUtbetalingstidslinje.disjoint(utbetalingstidslinje)

    return segmenterFjernet.toList().map {
        Opphørsperiode(
                periodeFom = it.fom,
                periodeTom = it.tom,
                vedtaksperiodetype = Vedtaksperiodetype.OPPHØR
        )
    }
}