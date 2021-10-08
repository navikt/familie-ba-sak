package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import java.time.LocalDate

data class Opphørsperiode(
    override val periodeFom: LocalDate,
    override val periodeTom: LocalDate?,
    override val vedtaksperiodetype: Vedtaksperiodetype = Vedtaksperiodetype.OPPHØR,
) : Vedtaksperiode

fun mapTilOpphørsperioder(
    forrigePersonopplysningGrunnlag: PersonopplysningGrunnlag? = null,
    forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse> = emptyList(),
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    endredeUtbetalingAndeler: List<EndretUtbetalingAndel>,
): List<Opphørsperiode> {
    val forrigeUtbetalingsperioder = if (forrigePersonopplysningGrunnlag != null) mapTilUtbetalingsperioder(
        personopplysningGrunnlag = forrigePersonopplysningGrunnlag,
        andelerTilkjentYtelse = forrigeAndelerTilkjentYtelse,
        endredeUtbetalingAndeler = endredeUtbetalingAndeler
    ) else emptyList()
    val utbetalingsperioder =
        mapTilUtbetalingsperioder(personopplysningGrunnlag, andelerTilkjentYtelse, endredeUtbetalingAndeler)

    val alleOpphørsperioder = if (forrigeUtbetalingsperioder.isNotEmpty() && utbetalingsperioder.isEmpty()) {
        listOf(
            Opphørsperiode(
                periodeFom = forrigeUtbetalingsperioder.minOf { it.periodeFom },
                periodeTom = forrigeUtbetalingsperioder.maxOf { it.periodeTom }
            )
        )
    } else if (utbetalingsperioder.isEmpty()) {
        emptyList()
    } else {
        listOf(
            finnOpphørsperioderPåGrunnAvReduksjonIRevurdering(
                forrigeUtbetalingsperioder = forrigeUtbetalingsperioder,
                utbetalingsperioder = utbetalingsperioder
            ),
            finnOpphørsperioderMellomUtbetalingsperioder(utbetalingsperioder),
            finnOpphørsperiodeEtterSisteUtbetalingsperiode(utbetalingsperioder)
        ).flatten()
    }.sortedBy { it.periodeFom }

    return slåSammenOpphørsperioder(alleOpphørsperioder)
}

fun slåSammenOpphørsperioder(alleOpphørsperioder: List<Opphørsperiode>): List<Opphørsperiode> {
    if (alleOpphørsperioder.isEmpty()) return emptyList()

    val sortertOpphørsperioder = alleOpphørsperioder.sortedBy { it.periodeFom }

    return sortertOpphørsperioder.fold(
        mutableListOf(
            sortertOpphørsperioder.first()
        )
    ) { acc: MutableList<Opphørsperiode>, nesteOpphørsperiode: Opphørsperiode ->
        val forrigeOpphørsperiode = acc.last()
        when {
            nesteOpphørsperiode.periodeFom.isSameOrBefore(forrigeOpphørsperiode.periodeTom ?: TIDENES_ENDE) -> {
                acc[acc.lastIndex] =
                    forrigeOpphørsperiode.copy(
                        periodeTom = maxOfOpphørsperiodeTom(
                            forrigeOpphørsperiode.periodeTom,
                            nesteOpphørsperiode.periodeTom
                        )
                    )
            }
            else -> {
                acc.add(nesteOpphørsperiode)
            }
        }

        acc
    }
}

private fun maxOfOpphørsperiodeTom(a: LocalDate?, b: LocalDate?): LocalDate? {
    return if (a != null && b != null) maxOf(a, b) else null
}

private fun finnOpphørsperioderMellomUtbetalingsperioder(utbetalingsperioder: List<Utbetalingsperiode>): List<Opphørsperiode> {
    val helYtelseTidslinje = LocalDateTimeline(
        listOf(
            LocalDateSegment(
                utbetalingsperioder.minOf { it.periodeFom },
                utbetalingsperioder.maxOf { it.periodeTom },
                null
            )
        )
    )

    return utledSegmenterFjernetOgMapTilOpphørsperioder(utbetalingsperioder, helYtelseTidslinje)
}

private fun finnOpphørsperiodeEtterSisteUtbetalingsperiode(utbetalingsperioder: List<Utbetalingsperiode>): List<Opphørsperiode> {
    val sisteUtbetalingsperiodeTom = utbetalingsperioder.maxOf { it.periodeTom }.toYearMonth()
    val nesteMåned = inneværendeMåned().nesteMåned()

    return if (sisteUtbetalingsperiodeTom.isBefore(nesteMåned)) {
        listOf(
            Opphørsperiode(
                periodeFom = sisteUtbetalingsperiodeTom.nesteMåned().førsteDagIInneværendeMåned(),
                periodeTom = null,
                vedtaksperiodetype = Vedtaksperiodetype.OPPHØR
            )
        )
    } else {
        emptyList()
    }
}

private fun finnOpphørsperioderPåGrunnAvReduksjonIRevurdering(
    forrigeUtbetalingsperioder: List<Utbetalingsperiode>,
    utbetalingsperioder: List<Utbetalingsperiode>
): List<Opphørsperiode> {
    val forrigeUtbetalingstidslinje = LocalDateTimeline(forrigeUtbetalingsperioder.map { it.tilTomtSegment() })

    return utledSegmenterFjernetOgMapTilOpphørsperioder(utbetalingsperioder, forrigeUtbetalingstidslinje)
}

private fun utledSegmenterFjernetOgMapTilOpphørsperioder(
    utbetalingsperioder: List<Utbetalingsperiode>,
    sammenligningstidslinje: LocalDateTimeline<Nothing?>
): List<Opphørsperiode> {
    val utbetalingstidslinje = LocalDateTimeline(utbetalingsperioder.map { it.tilTomtSegment() })
    val segmenterFjernet = sammenligningstidslinje.disjoint(utbetalingstidslinje).compress()

    return segmenterFjernet.toList().map {
        Opphørsperiode(
            periodeFom = it.fom,
            periodeTom = it.tom,
            vedtaksperiodetype = Vedtaksperiodetype.OPPHØR
        )
    }
}
