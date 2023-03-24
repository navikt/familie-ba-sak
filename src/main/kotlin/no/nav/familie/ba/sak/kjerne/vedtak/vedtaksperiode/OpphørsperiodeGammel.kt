package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.lagVertikaleSegmenter
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import java.time.LocalDate

fun mapTilOpphørsperioderGammel(
    forrigePersonopplysningGrunnlag: PersonopplysningGrunnlag? = null,
    forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger> = emptyList(),
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
): List<Opphørsperiode> {
    val forrigeUtbetalingsperioder = if (forrigePersonopplysningGrunnlag != null) {
        mapTilUtbetalingsperioderGammel(
            personopplysningGrunnlag = forrigePersonopplysningGrunnlag,
            andelerTilkjentYtelse = forrigeAndelerTilkjentYtelse
        )
    } else {
        emptyList()
    }
    val utbetalingsperioder =
        mapTilUtbetalingsperioderGammel(personopplysningGrunnlag, andelerTilkjentYtelse)

    return if (forrigeUtbetalingsperioder.isNotEmpty() && utbetalingsperioder.isEmpty()) {
        listOf(
            Opphørsperiode(
                periodeFom = forrigeUtbetalingsperioder.minOf { it.periodeFom },
                periodeTom = forrigeUtbetalingsperioder.maxOf { it.periodeTom }
            )
        )
    } else {
        if (utbetalingsperioder.isEmpty()) {
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
    }
}

fun slåSammenOpphørsperioderGammel(alleOpphørsperioder: List<Opphørsperiode>): List<Opphørsperiode> {
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

fun mapTilUtbetalingsperioderGammel(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
): List<Utbetalingsperiode> {
    return andelerTilkjentYtelse.lagVertikaleSegmenter().map { (segment, andelerForSegment) ->
        Utbetalingsperiode(
            periodeFom = segment.fom,
            periodeTom = segment.tom,
            ytelseTyper = andelerForSegment.map(AndelTilkjentYtelseMedEndreteUtbetalinger::type),
            utbetaltPerMnd = segment.value,
            antallBarn = andelerForSegment.count { andel ->
                personopplysningGrunnlag.barna.any { barn -> barn.aktør == andel.aktør }
            },
            utbetalingsperiodeDetaljer = andelerForSegment.lagUtbetalingsperiodeDetaljerGammel(personopplysningGrunnlag)
        )
    }
}

internal fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.lagUtbetalingsperiodeDetaljerGammel(
    personopplysningGrunnlag: PersonopplysningGrunnlag
): List<UtbetalingsperiodeDetalj> =
    this.map { andel ->
        val personForAndel =
            personopplysningGrunnlag.søkerOgBarn.find { person -> andel.aktør == person.aktør }
                ?: throw IllegalStateException("Fant ikke personopplysningsgrunnlag for andel")
        UtbetalingsperiodeDetalj(
            person = personForAndel.tilRestPerson(),
            ytelseType = andel.type,
            utbetaltPerMnd = andel.kalkulertUtbetalingsbeløp,
            erPåvirketAvEndring = andel.endreteUtbetalinger.isNotEmpty(),
            prosent = andel.prosent
        )
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
