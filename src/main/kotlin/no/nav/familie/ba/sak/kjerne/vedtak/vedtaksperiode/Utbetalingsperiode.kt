package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.ekstern.restDomene.RestPerson
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPerson
import no.nav.familie.ba.sak.kjerne.beregning.beregnUtbetalingsperioderUtenKlassifisering
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
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
) : Vedtaksperiode {
    fun tilTomtSegment() = LocalDateSegment(
        this.periodeFom,
        this.periodeTom,
        null
    )
}

fun hentUtbetalingsperiodeForVedtaksperiode(
    utbetalingsperioder: List<Utbetalingsperiode>,
    fom: LocalDate?
): Utbetalingsperiode {
    if (utbetalingsperioder.isEmpty()) {
        throw Feil("Det finnes ingen utbetalingsperioder ved utledning av utbetalingsperiode.")
    }
    val fomDato = fom?.toYearMonth() ?: inneværendeMåned()

    val sorterteUtbetalingsperioder = utbetalingsperioder.sortedBy { it.periodeFom }

    return sorterteUtbetalingsperioder.lastOrNull { it.periodeFom.toYearMonth() <= fomDato }
        ?: sorterteUtbetalingsperioder.firstOrNull()
        ?: throw Feil("Finner ikke gjeldende utbetalingsperiode ved fortsatt innvilget")
}

fun hentPersonIdenterFraUtbetalingsperioder(utbetalingsperioder: List<Utbetalingsperiode>): List<String> {
    return hentUtbetalingsperiodeForVedtaksperiode(
        utbetalingsperioder,
        null
    ).utbetalingsperiodeDetaljer.map { it.person.personIdent }
}

data class UtbetalingsperiodeDetalj(
    val person: RestPerson,
    val ytelseType: YtelseType,
    val utbetaltPerMnd: Int,
    val erPåvirketAvEndring: Boolean,
)

fun mapTilUtbetalingsperioder(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    filterAndeler: (andelerTilkjentYtelse: List<AndelTilkjentYtelse>) -> List<AndelTilkjentYtelse> = { it }
): List<Utbetalingsperiode> {
    return filterAndeler(andelerTilkjentYtelse).utledSegmenter().map { segment ->
        val andelerForSegment = andelerTilkjentYtelse.filter {
            segment.localDateInterval.overlaps(
                LocalDateInterval(
                    it.stønadFom.førsteDagIInneværendeMåned(),
                    it.stønadTom.sisteDagIInneværendeMåned()
                )
            )
        }

        Utbetalingsperiode(
            periodeFom = segment.fom,
            periodeTom = segment.tom,
            ytelseTyper = andelerForSegment.map(AndelTilkjentYtelse::type),
            utbetaltPerMnd = segment.value,
            antallBarn = andelerForSegment.count { andel ->
                personopplysningGrunnlag.barna.any { barn -> barn.personIdent.ident == andel.personIdent }
            },
            utbetalingsperiodeDetaljer = andelerForSegment.lagUtbetalingsperiodeDetaljer(personopplysningGrunnlag)
        )
    }
}

private fun List<AndelTilkjentYtelse>.utledSegmenter(): List<LocalDateSegment<Int>> {
    val utbetalingsPerioder = beregnUtbetalingsperioderUtenKlassifisering(this.toSet())
    return utbetalingsPerioder.toSegments()
        .sortedWith(compareBy<LocalDateSegment<Int>>({ it.fom }, { it.value }, { it.tom }))
}

private fun List<AndelTilkjentYtelse>.lagUtbetalingsperiodeDetaljer(personopplysningGrunnlag: PersonopplysningGrunnlag): List<UtbetalingsperiodeDetalj> =
    this.map { andel ->
        val personForAndel =
            personopplysningGrunnlag.personer.find { person -> andel.personIdent == person.personIdent.ident }
                ?: throw IllegalStateException("Fant ikke personopplysningsgrunnlag for andel")

        UtbetalingsperiodeDetalj(
            person = personForAndel.tilRestPerson(),
            ytelseType = andel.type,
            utbetaltPerMnd = andel.kalkulertUtbetalingsbeløp,
            erPåvirketAvEndring = andel.endretUtbetalingAndeler.isNotEmpty()
        )
    }
