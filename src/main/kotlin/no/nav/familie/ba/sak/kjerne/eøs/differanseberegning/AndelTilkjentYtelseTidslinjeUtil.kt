package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MAX_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MIN_MÅNED
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import java.time.YearMonth

fun Iterable<AndelTilkjentYtelse>.tilSeparateTidslinjerForBarna(): Map<Aktør, Tidslinje<AndelTilkjentYtelse, Måned>> {
    return this
        .filter { !it.erSøkersAndel() }
        .groupBy { it.aktør }
        .mapValues { (_, andeler) -> tidslinje { andeler.map { it.tilPeriode() } } }
}

fun Map<Aktør, Tidslinje<AndelTilkjentYtelse, Måned>>.tilAndelerTilkjentYtelse(): List<AndelTilkjentYtelse> {
    return this.values.flatMap { it.tilAndelTilkjentYtelse() }
}

fun Iterable<Tidslinje<AndelTilkjentYtelse, Måned>>.tilAndelerTilkjentYtelse(): List<AndelTilkjentYtelse> {
    return this.flatMap { it.tilAndelTilkjentYtelse() }
}

fun Tidslinje<AndelTilkjentYtelse, Måned>.tilAndelTilkjentYtelse(): List<AndelTilkjentYtelse> {
    return this
        .perioder().map {
            it.innhold?.medPeriode(
                it.fraOgMed.tilYearMonth(),
                it.tilOgMed.tilYearMonth()
            )
        }.filterNotNull()
}

fun AndelTilkjentYtelse.tilPeriode() = Periode(
    this.stønadFom.tilTidspunkt(),
    this.stønadTom.tilTidspunkt(),
    // Ta bort periode, slik at det ikke blir med på innholdet som vurderes for likhet
    this.medPeriode(null, null)
)

fun AndelTilkjentYtelse.medPeriode(fraOgMed: YearMonth?, tilOgMed: YearMonth?) =
    copy(
        id = 0,
        stønadFom = fraOgMed ?: MIN_MÅNED,
        stønadTom = tilOgMed ?: MAX_MÅNED
    ).also { versjon = this.versjon }
