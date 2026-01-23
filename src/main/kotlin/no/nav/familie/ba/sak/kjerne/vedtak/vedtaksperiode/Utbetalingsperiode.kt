package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.restDomene.PersonDto
import no.nav.familie.ba.sak.ekstern.restDomene.tilPersonDto
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.tilTidslinje
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
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
    val person: PersonDto,
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
            personopplysningGrunnlag.søkerOgBarn.find { person -> andel.aktør == person.aktør }?.tilPersonDto(eldsteBarnsFødselsdato = personopplysningGrunnlag.eldsteBarnSinFødselsdato)
                ?: throw Feil("Fant ikke personopplysningsgrunnlag for andel"),
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
        andelerTidslinjePerAktørOgType
            .tilPerioderIkkeNull()
            .filter { !it.verdi.isEmpty() }
            .map { periode ->
                Utbetalingsperiode(
                    periodeFom = periode.fom ?: throw Feil("Fra og med-dato kan ikke være null"),
                    periodeTom = periode.tom ?: throw Feil("Til og med-dato kan ikke være null"),
                    ytelseTyper = periode.verdi.map { andelTilkjentYtelse -> andelTilkjentYtelse.type },
                    utbetaltPerMnd = periode.verdi.sumOf { andelTilkjentYtelse -> andelTilkjentYtelse.kalkulertUtbetalingsbeløp },
                    antallBarn =
                        periode.verdi
                            .map { it.aktør }
                            .toSet()
                            .count { aktør -> personopplysningGrunnlag.barna.any { barn -> barn.aktør == aktør } },
                    utbetalingsperiodeDetaljer = periode.verdi.lagUtbetalingsperiodeDetaljer(personopplysningGrunnlag),
                )
            }

    return utbetalingsPerioder
}

private fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilKombinertTidslinjePerAktørOgType(): Tidslinje<Collection<AndelTilkjentYtelseMedEndreteUtbetalinger>> {
    val andelTilkjentYtelsePerPersonOgType = groupBy { Pair(it.aktør, it.type) }

    val andelTilkjentYtelsePerPersonOgTypeTidslinjer =
        andelTilkjentYtelsePerPersonOgType.values.map { it.tilTidslinje() }

    return andelTilkjentYtelsePerPersonOgTypeTidslinjer.kombiner { it.toList() }
}

fun Collection<AndelTilkjentYtelseMedEndreteUtbetalinger>.lagUtbetalingsperiodeDetaljer(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
): List<UtbetalingsperiodeDetalj> =
    this.map { andel ->
        val personForAndel =
            personopplysningGrunnlag.personer.find { person -> andel.aktør == person.aktør }
                ?: throw Feil("Fant ikke personopplysningsgrunnlag for andel")

        UtbetalingsperiodeDetalj(
            person = personForAndel.tilPersonDto(eldsteBarnsFødselsdato = personopplysningGrunnlag.eldsteBarnSinFødselsdato),
            ytelseType = andel.type,
            utbetaltPerMnd = andel.kalkulertUtbetalingsbeløp,
            erPåvirketAvEndring = andel.endreteUtbetalinger.isNotEmpty(),
            prosent = andel.prosent,
            endringsårsak = andel.endreteUtbetalinger.singleOrNull()?.årsak,
        )
    }
