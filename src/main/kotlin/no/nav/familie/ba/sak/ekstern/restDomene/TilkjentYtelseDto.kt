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

data class PersonMedAndelerDto(
    val personIdent: String?,
    val beløp: Int,
    val stønadFom: YearMonth,
    val stønadTom: YearMonth,
    val ytelsePerioder: List<YtelsePeriodeDto>,
)

data class YtelsePeriodeDto(
    val beløp: Int,
    val stønadFom: YearMonth,
    val stønadTom: YearMonth,
    val ytelseType: YtelseType,
    val skalUtbetales: Boolean,
)

fun PersonopplysningGrunnlag.tilPersonerMedAndelerDto(andelerKnyttetTilPersoner: List<AndelTilkjentYtelse>): List<PersonMedAndelerDto> =
    andelerKnyttetTilPersoner
        .groupBy { it.aktør }
        .map { andelerForPerson ->
            val personId = andelerForPerson.key
            val andeler = andelerForPerson.value

            val ytelsePerioder = andeler.tilYtelsePerioderDto()

            PersonMedAndelerDto(
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

fun List<AndelTilkjentYtelse>.tilYtelsePerioderDto(): List<YtelsePeriodeDto> {
    val ytelsePeriodeDtoTidslinjePerAktørOgTypeSlåttSammen =
        this
            .groupBy { Pair(it.aktør, it.type) }
            .mapValues { (_, andelerTilkjentYtelse) ->
                andelerTilkjentYtelse
                    .tilYtelsePeriodeUtenDatoerTidslinjeDto()
                    .slåSammenLikePerioder()
            }

    return ytelsePeriodeDtoTidslinjePerAktørOgTypeSlåttSammen
        .flatMap { (_, andelerTidslinje) -> andelerTidslinje.tilPerioderIkkeNull() }
        .map { periode ->
            periode.verdi.let { innhold ->
                YtelsePeriodeDto(
                    beløp = innhold.kalkulertUtbetalingsbeløp,
                    stønadFom = periode.fom?.toYearMonth() ?: throw Feil("Fra og med-dato kan ikke være null"),
                    stønadTom = periode.tom?.toYearMonth() ?: throw Feil("Til og med-dato kan ikke være null"),
                    ytelseType = innhold.ytelseType,
                    skalUtbetales = innhold.skalUtbetales,
                )
            }
        }
}

data class YtelsePeriodeUtenDatoerDto(
    val kalkulertUtbetalingsbeløp: Int,
    val ytelseType: YtelseType,
    val skalUtbetales: Boolean,
)

private fun List<AndelTilkjentYtelse>.tilYtelsePeriodeUtenDatoerTidslinjeDto() =
    this
        .map {
            Periode(
                fom = it.stønadFom.førsteDagIInneværendeMåned(),
                tom = it.stønadTom.sisteDagIInneværendeMåned(),
                verdi =
                    YtelsePeriodeUtenDatoerDto(
                        kalkulertUtbetalingsbeløp = it.kalkulertUtbetalingsbeløp,
                        ytelseType = it.type,
                        skalUtbetales = it.prosent > BigDecimal.ZERO,
                    ),
            )
        }.tilTidslinje()
