package no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import java.time.LocalDate

data class Opphørsperiode(
        override val periodeFom: LocalDate,
        override val periodeTom: LocalDate?,
        override val vedtaksperiodetype: Vedtaksperiodetype = Vedtaksperiodetype.OPPHØR
) : Vedtaksperiode

fun mapTilOpphørsperioder(forrigePersonopplysningGrunnlag: PersonopplysningGrunnlag? = null,
                          forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse> = emptyList(),
                          personopplysningGrunnlag: PersonopplysningGrunnlag,
                          andelerTilkjentYtelse: List<AndelTilkjentYtelse>): List<Opphørsperiode> {
    val forrigeUtbetalingsperioder = if (forrigePersonopplysningGrunnlag != null) mapTilUtbetalingsperioder(
            forrigePersonopplysningGrunnlag,
            forrigeAndelerTilkjentYtelse) else emptyList()
    val utbetalingsperioder = mapTilUtbetalingsperioder(personopplysningGrunnlag, andelerTilkjentYtelse)

    val alleOpphørsperioder = if (forrigeUtbetalingsperioder.isNotEmpty() && utbetalingsperioder.isEmpty()) {
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
    }.sortedBy { it.periodeFom }

    return slåSammenOpphørsperioder(alleOpphørsperioder)
}

private fun slåSammenOpphørsperioder(alleOpphørsperioder: List<Opphørsperiode>) = alleOpphørsperioder.fold(mutableListOf(
        alleOpphørsperioder.first())) { acc: MutableList<Opphørsperiode>, opphørsperiode: Opphørsperiode ->
    val forrigeOpphørsperiode = acc.last()
    when {
        opphørsperiode.periodeFom.isSameOrBefore(forrigeOpphørsperiode.periodeTom ?: TIDENES_ENDE) -> {
            acc.removeLast()
            acc.add(opphørsperiode.copy(periodeFom = forrigeOpphørsperiode.periodeFom))
        }
        (opphørsperiode.periodeTom ?: TIDENES_ENDE).isSameOrBefore(forrigeOpphørsperiode.periodeTom ?: TIDENES_ENDE) -> {
            acc.removeLast()
            acc.add(opphørsperiode.copy(periodeFom = forrigeOpphørsperiode.periodeFom))
        }
        else -> {
            acc.add(opphørsperiode)
        }
    }

    acc
}

private fun finnOpphørsperioderMellomUtbetalingsperioder(utbetalingsperioder: List<Utbetalingsperiode>): List<Opphørsperiode> {
    val helYtelseTidslinje = LocalDateTimeline(
            listOf(LocalDateSegment(
                    utbetalingsperioder.minOf { it.periodeFom },
                    utbetalingsperioder.maxOf { it.periodeTom },
                    null
            )))

    return utledSegmenterFjernetOgMapTilOpphørsperioder(utbetalingsperioder, helYtelseTidslinje)
}

private fun finnOpphørsperiodeEtterSisteUtbetalingsperiode(utbetalingsperioder: List<Utbetalingsperiode>): List<Opphørsperiode> {
    val sisteUtbetalingsperiodeTom = utbetalingsperioder.maxOf { it.periodeTom }.toYearMonth()
    val nesteMåned = inneværendeMåned().nesteMåned()

    return if (sisteUtbetalingsperiodeTom.isBefore(nesteMåned)) {
        listOf(Opphørsperiode(
                periodeFom = sisteUtbetalingsperiodeTom.nesteMåned().førsteDagIInneværendeMåned(),
                periodeTom = null,
                vedtaksperiodetype = Vedtaksperiodetype.OPPHØR
        ))
    } else {
        emptyList()
    }
}

private fun finnOpphørsperioderPåGrunnAvReduksjonIRevurdering(forrigeUtbetalingsperioder: List<Utbetalingsperiode>,
                                                              utbetalingsperioder: List<Utbetalingsperiode>): List<Opphørsperiode> {
    val forrigeUtbetalingstidslinje = LocalDateTimeline(forrigeUtbetalingsperioder.map { it.tilTomtSegment() })

    return utledSegmenterFjernetOgMapTilOpphørsperioder(utbetalingsperioder, forrigeUtbetalingstidslinje)
}

private fun utledSegmenterFjernetOgMapTilOpphørsperioder(utbetalingsperioder: List<Utbetalingsperiode>,
                                                         sammenligningstidslinje: LocalDateTimeline<Nothing?>): List<Opphørsperiode> {
    val utbetalingstidslinje = LocalDateTimeline(utbetalingsperioder.map { it.tilTomtSegment() })

    val segmenterFjernet = sammenligningstidslinje.disjoint(utbetalingstidslinje)

    return segmenterFjernet.toList().map {
        Opphørsperiode(
                periodeFom = it.fom,
                periodeTom = it.tom,
                vedtaksperiodetype = Vedtaksperiodetype.OPPHØR
        )
    }
}