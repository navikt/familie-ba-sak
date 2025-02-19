package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MAX_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MIN_MÅNED
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.komposisjon.erTilogMed3ÅrTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.komposisjon.joinIkkeNull
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.math.BigDecimal
import java.time.YearMonth
import no.nav.familie.tidslinje.Periode as FamilieFellesPeriode
import no.nav.familie.tidslinje.Tidslinje as FamilieFellesTidslinje

fun Iterable<AndelTilkjentYtelse>.tilSeparateTidslinjerForBarna(): Map<Aktør, FamilieFellesTidslinje<AndelTilkjentYtelse>> =
    this
        .filter { !it.erSøkersAndel() }
        .groupBy { it.aktør }
        .mapValues { (_, andeler) -> andeler.map { it.tilPeriode() }.tilTidslinje() }

fun Map<Aktør, FamilieFellesTidslinje<AndelTilkjentYtelse>>.tilAndelerTilkjentYtelse(): List<AndelTilkjentYtelse> = this.values.flatMap { it.tilAndelTilkjentYtelse() }

fun Iterable<Tidslinje<AndelTilkjentYtelse, Måned>>.tilAndelerTilkjentYtelse(): List<AndelTilkjentYtelse> = this.flatMap { it.tilAndelTilkjentYtelse() }

fun Tidslinje<AndelTilkjentYtelse, Måned>.tilAndelTilkjentYtelse(): List<AndelTilkjentYtelse> =
    this
        .perioder()
        .map {
            it.innhold?.medPeriode(
                it.fraOgMed.tilYearMonth(),
                it.tilOgMed.tilYearMonth(),
            )
        }.filterNotNull()

fun FamilieFellesTidslinje<AndelTilkjentYtelse>.tilAndelTilkjentYtelse(): List<AndelTilkjentYtelse> =
    this
        .tilPerioderIkkeNull()
        .map {
            it.verdi.medPeriode(
                it.fom?.toYearMonth(),
                it.tom?.toYearMonth(),
            )
        }

fun AndelTilkjentYtelse.tilPeriode() =
    FamilieFellesPeriode(
        // Ta bort periode, slik at det ikke blir med på innholdet som vurderes for likhet
        verdi = this.medPeriode(null, null),
        fom = this.stønadFom.førsteDagIInneværendeMåned(),
        tom = this.stønadTom.sisteDagIInneværendeMåned(),
    )

fun AndelTilkjentYtelse.medPeriode(
    fraOgMed: YearMonth?,
    tilOgMed: YearMonth?,
) = copy(
    id = 0,
    stønadFom = fraOgMed ?: MIN_MÅNED,
    stønadTom = tilOgMed ?: MAX_MÅNED,
).also { versjon = this.versjon }

/**
 * Ivaretar fom og tom, slik at eventuelle splitter blir med videre.
 */
fun Iterable<AndelTilkjentYtelse>.tilTidslinjeForSøkersYtelse(ytelseType: YtelseType) =
    this
        .filter { it.erSøkersAndel() }
        .filter { it.type == ytelseType }
        .let {
            tidslinje {
                it.map { Periode(it.stønadFom.tilTidspunkt(), it.stønadTom.tilTidspunkt(), it) }
            }
        }

fun Iterable<AndelTilkjentYtelse>.tilFamilieFellesTidslinjeForSøkersYtelse(ytelseType: YtelseType) =
    this
        .filter { it.erSøkersAndel() }
        .filter { it.type == ytelseType }
        .map {
            FamilieFellesPeriode(
                verdi = it,
                fom = it.stønadFom.førsteDagIInneværendeMåned(),
                tom = it.stønadTom.sisteDagIInneværendeMåned(),
            )
        }.tilTidslinje()

fun Map<Aktør, FamilieFellesTidslinje<AndelTilkjentYtelse>>.kunAndelerTilOgMed3År(barna: List<Person>): Map<Aktør, FamilieFellesTidslinje<AndelTilkjentYtelse>> {
    val barnasErInntil3ÅrTidslinjer = barna.associate { it.aktør to erTilogMed3ÅrTidslinje(it.fødselsdato) }

    // For hvert barn kombiner andel-tidslinjen med 3-års-tidslinjen. Resultatet er andelene når barna er inntil 3 år
    return this.joinIkkeNull(barnasErInntil3ÅrTidslinjer) { andel, _ -> andel }
}

data class AndelTilkjentYtelseForTidslinje(
    val aktør: Aktør,
    val beløp: Int,
    val sats: Int,
    val ytelseType: YtelseType,
    val prosent: BigDecimal,
    val nasjonaltPeriodebeløp: Int = beløp,
    val differanseberegnetPeriodebeløp: Int? = null,
)

fun AndelTilkjentYtelse.tilpassTilTidslinje() =
    AndelTilkjentYtelseForTidslinje(
        aktør = this.aktør,
        beløp = this.kalkulertUtbetalingsbeløp,
        ytelseType = this.type,
        sats = this.sats,
        prosent = this.prosent,
        nasjonaltPeriodebeløp = this.nasjonaltPeriodebeløp ?: this.kalkulertUtbetalingsbeløp,
        differanseberegnetPeriodebeløp = this.differanseberegnetPeriodebeløp,
    )

fun Tidslinje<AndelTilkjentYtelseForTidslinje, Måned>.tilAndelerTilkjentYtelse(tilkjentYtelse: TilkjentYtelse) =
    perioder()
        .filter { it.innhold != null }
        .map {
            AndelTilkjentYtelse(
                behandlingId = tilkjentYtelse.behandling.id,
                tilkjentYtelse = tilkjentYtelse,
                aktør = it.innhold!!.aktør,
                type = it.innhold.ytelseType,
                kalkulertUtbetalingsbeløp = it.innhold.beløp,
                nasjonaltPeriodebeløp = it.innhold.nasjonaltPeriodebeløp,
                differanseberegnetPeriodebeløp = it.innhold.differanseberegnetPeriodebeløp,
                sats = it.innhold.sats,
                prosent = it.innhold.prosent,
                stønadFom = it.fraOgMed.tilYearMonth(),
                stønadTom = it.tilOgMed.tilYearMonth(),
            )
        }

fun FamilieFellesTidslinje<AndelTilkjentYtelseForTidslinje>.tilAndelerTilkjentYtelse(tilkjentYtelse: TilkjentYtelse) =
    tilPerioderIkkeNull()
        .map {
            AndelTilkjentYtelse(
                behandlingId = tilkjentYtelse.behandling.id,
                tilkjentYtelse = tilkjentYtelse,
                aktør = it.verdi.aktør,
                type = it.verdi.ytelseType,
                kalkulertUtbetalingsbeløp = it.verdi.beløp,
                nasjonaltPeriodebeløp = it.verdi.nasjonaltPeriodebeløp,
                differanseberegnetPeriodebeløp = it.verdi.differanseberegnetPeriodebeløp,
                sats = it.verdi.sats,
                prosent = it.verdi.prosent,
                stønadFom = it.fom?.toYearMonth() ?: throw Feil("Fra og med-dato kan ikke være null i AndelTilkjentYtelse"),
                stønadTom = it.tom?.toYearMonth() ?: throw Feil("Til og med-dato kan ikke være null i AndelTilkjentYtelse"),
            )
        }

/**
 * Lager tidslinje med AndelTilkjentYtelseForTidslinje-objekter, som derfor er "trygg" mtp DB-endringer
 */
fun Iterable<AndelTilkjentYtelse>.tilTryggTidslinjeForSøkersYtelse(ytelseType: YtelseType) =
    this
        .filter { it.erSøkersAndel() }
        .filter { it.type == ytelseType }
        .map {
            FamilieFellesPeriode(
                verdi = it.tilpassTilTidslinje(),
                fom = it.stønadFom.førsteDagIInneværendeMåned(),
                tom = it.stønadTom.sisteDagIInneværendeMåned(),
            )
        }.tilTidslinje()
