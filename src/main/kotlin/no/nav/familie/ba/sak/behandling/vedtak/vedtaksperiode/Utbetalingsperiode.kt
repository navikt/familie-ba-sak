package no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.restDomene.RestPerson
import no.nav.familie.ba.sak.behandling.restDomene.tilRestPerson
import no.nav.familie.ba.sak.beregning.beregnUtbetalingsperioderUtenKlassifisering
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import java.time.LocalDate

/**
 * Dataklasser som brukes til frontend og backend når man jobber med vertikale utbetalingsperioder
 */
data class Utbetalingsperiode(
        override val periodeFom: LocalDate,
        override val periodeTom: LocalDate,
        override val vedtaksperiodetype: Vedtaksperiodetype = Vedtaksperiodetype.UTBETALING,
        val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj>,
        val ytelseTyper: List<YtelseType>,
        val antallBarn: Int,
        val utbetaltPerMnd: Int,
) : Vedtaksperiode

data class UtbetalingsperiodeDetalj(
        val person: RestPerson,
        val ytelseType: YtelseType,
        val utbetaltPerMnd: Int,
)

fun mapTilUtbetalingsperioder(personopplysningGrunnlag: PersonopplysningGrunnlag,
                              andelerTilkjentYtelse: List<AndelTilkjentYtelse>): List<Utbetalingsperiode> {
    return if (andelerTilkjentYtelse.isEmpty()) {
        emptyList()
    } else {
        val segmenter = utledSegmenterFraAndeler(andelerTilkjentYtelse.toSet())

        segmenter.map { segment ->
            val andelerForSegment = andelerTilkjentYtelse.filter {
                segment.localDateInterval.overlaps(LocalDateInterval(it.stønadFom.førsteDagIInneværendeMåned(),
                                                                     it.stønadTom.sisteDagIInneværendeMåned()))
            }
            mapTilUtbetalingsperiode(segment = segment,
                                     andelerForSegment = andelerForSegment,
                                     personopplysningGrunnlag = personopplysningGrunnlag)
        }
    }
}

private fun utledSegmenterFraAndeler(andelerTilkjentYtelse: Set<AndelTilkjentYtelse>): List<LocalDateSegment<Int>> {
    val utbetalingsPerioder = beregnUtbetalingsperioderUtenKlassifisering(andelerTilkjentYtelse)
    return utbetalingsPerioder.toSegments().sortedWith(compareBy<LocalDateSegment<Int>>({ it.fom }, { it.value }, { it.tom }))
}

private fun mapTilUtbetalingsperiode(segment: LocalDateSegment<Int>,
                                     andelerForSegment: List<AndelTilkjentYtelse>,
                                     personopplysningGrunnlag: PersonopplysningGrunnlag): Utbetalingsperiode {
    val utbetalingsperiodeDetaljer = andelerForSegment.map { andel ->
        val personForAndel =
                personopplysningGrunnlag.personer.find { person -> andel.personIdent == person.personIdent.ident }
                ?: throw IllegalStateException("Fant ikke personopplysningsgrunnlag for andel")

        UtbetalingsperiodeDetalj(
                person = personForAndel.tilRestPerson(),
                ytelseType = andel.type,
                utbetaltPerMnd = andel.beløp
        )
    }

    return Utbetalingsperiode(
            periodeFom = segment.fom,
            periodeTom = segment.tom,
            ytelseTyper = andelerForSegment.map(AndelTilkjentYtelse::type),
            utbetaltPerMnd = segment.value,
            antallBarn = andelerForSegment.count { andel ->
                personopplysningGrunnlag.barna.any { barn -> barn.personIdent.ident == andel.personIdent }
            },
            utbetalingsperiodeDetaljer = utbetalingsperiodeDetaljer
    )
}