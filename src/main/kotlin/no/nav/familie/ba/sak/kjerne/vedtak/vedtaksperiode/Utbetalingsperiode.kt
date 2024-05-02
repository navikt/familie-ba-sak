package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.ekstern.restDomene.RestPerson
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPerson
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalingerTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilDagEllerFørsteDagIPerioden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilDagEllerSisteDagIPerioden
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Dataklasser som brukes til frontend og backend når man jobber med vertikale utbetalingsperioder
 */

data class Utbetalingsperiode(
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val vedtaksperiodetype: Vedtaksperiodetype = Vedtaksperiodetype.UTBETALING,
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj>,
    val ytelseTyper: List<YtelseType>,
    val antallBarn: Int,
    val utbetaltPerMnd: Int,
)

data class UtbetalingsperiodeDetalj(
    val person: RestPerson,
    val ytelseType: YtelseType,
    val utbetaltPerMnd: Int,
    val erPåvirketAvEndring: Boolean,
    val endringsårsak: Årsak?,
    val prosent: BigDecimal,
) {
    constructor(
        andel: AndelTilkjentYtelseMedEndreteUtbetalinger,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
    ) : this(
        person =
            personopplysningGrunnlag.søkerOgBarn.find { person -> andel.aktør == person.aktør }?.tilRestPerson()
                ?: throw IllegalStateException("Fant ikke personopplysningsgrunnlag for andel"),
        ytelseType = andel.type,
        utbetaltPerMnd = andel.kalkulertUtbetalingsbeløp,
        erPåvirketAvEndring = andel.endreteUtbetalinger.isNotEmpty(),
        endringsårsak = andel.endreteUtbetalinger.singleOrNull()?.årsak,
        prosent = andel.prosent,
    )
}

fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.mapTilUtbetalingsperioder(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
): List<Utbetalingsperiode> {
    val andelerTidslinjePerAktørOgType = this.tilKombinertTidslinjePerAktørOgType()

    val utbetalingsPerioder =
        andelerTidslinjePerAktørOgType.perioder()
            .filter { !it.innhold.isNullOrEmpty() }
            .map { periode ->
                Utbetalingsperiode(
                    periodeFom = periode.fraOgMed.tilDagEllerFørsteDagIPerioden().tilLocalDate(),
                    periodeTom = periode.tilOgMed.tilDagEllerSisteDagIPerioden().tilLocalDate(),
                    ytelseTyper = periode.innhold!!.map { andelTilkjentYtelse -> andelTilkjentYtelse.type },
                    utbetaltPerMnd = periode.innhold.sumOf { andelTilkjentYtelse -> andelTilkjentYtelse.kalkulertUtbetalingsbeløp },
                    antallBarn =
                        periode.innhold
                            .map { it.aktør }.toSet()
                            .count { aktør -> personopplysningGrunnlag.barna.any { barn -> barn.aktør == aktør } },
                    utbetalingsperiodeDetaljer = periode.innhold.lagUtbetalingsperiodeDetaljer(personopplysningGrunnlag),
                )
            }

    return utbetalingsPerioder
}

private fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilKombinertTidslinjePerAktørOgType(): Tidslinje<Collection<AndelTilkjentYtelseMedEndreteUtbetalinger>, Måned> {
    val andelTilkjentYtelsePerPersonOgType = groupBy { Pair(it.aktør, it.type) }

    val andelTilkjentYtelsePerPersonOgTypeTidslinjer =
        andelTilkjentYtelsePerPersonOgType.values.map { AndelTilkjentYtelseMedEndreteUtbetalingerTidslinje(it) }

    return andelTilkjentYtelsePerPersonOgTypeTidslinjer.kombiner { it.toList() }
}

fun Collection<AndelTilkjentYtelseMedEndreteUtbetalinger>.lagUtbetalingsperiodeDetaljer(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
): List<UtbetalingsperiodeDetalj> =
    this.map { andel ->
        val personForAndel =
            personopplysningGrunnlag.personer.find { person -> andel.aktør == person.aktør }
                ?: throw IllegalStateException("Fant ikke personopplysningsgrunnlag for andel")

        UtbetalingsperiodeDetalj(
            person = personForAndel.tilRestPerson(),
            ytelseType = andel.type,
            utbetaltPerMnd = andel.kalkulertUtbetalingsbeløp,
            erPåvirketAvEndring = andel.endreteUtbetalinger.isNotEmpty(),
            prosent = andel.prosent,
            endringsårsak = andel.endreteUtbetalinger.singleOrNull()?.årsak,
        )
    }
