package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.ekstern.restDomene.RestYtelsePeriode
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.månedPeriodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import java.math.BigDecimal
import java.time.YearMonth


internal data class BeregnetAndel(
    val person: Person,
    val stønadFom: YearMonth,
    val stønadTom: YearMonth,
    val beløp: Int,
    val sats: Int,
    val prosent: BigDecimal,
)

data class RestYtelsePeriodeUtenDatoer(
    val kalkulertUtbetalingsbeløp: Int,
    val ytelseType: YtelseType,
    val skalUtbetales: Boolean,
)

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

private fun List<AndelTilkjentYtelse>.tilRestYtelsePeriodeUtenDatoerTidslinje() =
    this
        .map {
            månedPeriodeAv(
                fraOgMed = it.stønadFom,
                tilOgMed = it.stønadTom,
                innhold = RestYtelsePeriodeUtenDatoer(kalkulertUtbetalingsbeløp = it.kalkulertUtbetalingsbeløp, it.type, it.prosent > BigDecimal.ZERO),
            )
        }.tilTidslinje()
