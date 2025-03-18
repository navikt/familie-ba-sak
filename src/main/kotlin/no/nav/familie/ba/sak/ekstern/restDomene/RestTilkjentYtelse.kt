package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class RestPersonMedAndeler(
    val personIdent: String?,
    val beløp: Int,
    val stønadFom: YearMonth,
    val stønadTom: YearMonth,
    val ytelsePerioder: List<RestYtelsePeriode>,
)

data class RestYtelsePeriode(
    val beløp: Int,
    val stønadFom: YearMonth,
    val stønadTom: YearMonth,
    val ytelseType: YtelseType,
    val skalUtbetales: Boolean,
)

fun PersonopplysningGrunnlag.tilRestPersonerMedAndeler(andelerKnyttetTilPersoner: List<AndelTilkjentYtelse>): List<RestPersonMedAndeler> =
    andelerKnyttetTilPersoner
        .groupBy { it.aktør }
        .map { andelerForPerson ->
            val personId = andelerForPerson.key
            val andeler = andelerForPerson.value

            val ytelsePerioder = andeler.tilRestYtelsePerioder()

            RestPersonMedAndeler(
                personIdent =
                    this.søkerOgBarn
                        .find { person -> person.aktør == personId }
                        ?.aktør
                        ?.aktivFødselsnummer(),
                beløp = ytelsePerioder.sumOf { it.beløp },
                stønadFom = ytelsePerioder.minOfOrNull { it.stønadFom } ?: LocalDate.MIN.toYearMonth(),
                stønadTom = ytelsePerioder.maxOfOrNull { it.stønadTom } ?: LocalDate.MAX.toYearMonth(),
                ytelsePerioder = ytelsePerioder,
            )
        }

fun List<AndelTilkjentYtelse>.tilRestYtelsePerioder(): List<RestYtelsePeriode> {
    val restYtelsePeriodeTidslinjePerAktørOgTypeSlåttSammen =
        this
            .groupBy { Pair(it.aktør, it.type) }
            .mapValues { (_, andelerTilkjentYtelse) ->
                andelerTilkjentYtelse
                    .tilRestYtelsePeriodeUtenDatoerTidslinje()
                    .slåSammenLikePerioder()
            }

    return restYtelsePeriodeTidslinjePerAktørOgTypeSlåttSammen
        .flatMap { (_, andelerTidslinje) -> andelerTidslinje.tilPerioderIkkeNull() }
        .map { periode ->
            periode.verdi.let { innhold ->
                RestYtelsePeriode(
                    beløp = innhold.kalkulertUtbetalingsbeløp,
                    stønadFom = periode.fom?.toYearMonth() ?: throw Feil("Fra og med-dato kan ikke være null"),
                    stønadTom = periode.tom?.toYearMonth() ?: throw Feil("Til og med-dato kan ikke være null"),
                    ytelseType = innhold.ytelseType,
                    skalUtbetales = innhold.skalUtbetales,
                )
            }
        }
}

data class RestYtelsePeriodeUtenDatoer(
    val kalkulertUtbetalingsbeløp: Int,
    val ytelseType: YtelseType,
    val skalUtbetales: Boolean,
)

private fun List<AndelTilkjentYtelse>.tilRestYtelsePeriodeUtenDatoerTidslinje() =
    this
        .map {
            Periode(
                fom = it.stønadFom.førsteDagIInneværendeMåned(),
                tom = it.stønadTom.sisteDagIInneværendeMåned(),
                verdi =
                    RestYtelsePeriodeUtenDatoer(
                        kalkulertUtbetalingsbeløp = it.kalkulertUtbetalingsbeløp,
                        ytelseType = it.type,
                        skalUtbetales = it.prosent > BigDecimal.ZERO,
                    ),
            )
        }.tilTidslinje()
