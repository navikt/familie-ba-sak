package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.månedPeriodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
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
                    .slåSammenLike()
            }

    return restYtelsePeriodeTidslinjePerAktørOgTypeSlåttSammen
        .flatMap { (_, andelerTidslinje) -> andelerTidslinje.perioder() }
        .mapNotNull { periode ->
            periode.innhold?.let { innhold ->
                RestYtelsePeriode(
                    beløp = innhold.kalkulertUtbetalingsbeløp,
                    stønadFom = periode.fraOgMed.tilYearMonth(),
                    stønadTom = periode.tilOgMed.tilYearMonth(),
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
            månedPeriodeAv(
                fraOgMed = it.stønadFom,
                tilOgMed = it.stønadTom,
                innhold =
                    RestYtelsePeriodeUtenDatoer(
                        kalkulertUtbetalingsbeløp = it.kalkulertUtbetalingsbeløp,
                        ytelseType = it.type,
                        skalUtbetales = it.prosent > BigDecimal.ZERO,
                    ),
            )
        }.tilTidslinje()
