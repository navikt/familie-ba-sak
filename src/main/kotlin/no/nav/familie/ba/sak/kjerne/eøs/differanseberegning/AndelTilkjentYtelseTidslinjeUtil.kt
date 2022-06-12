package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MAX_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MIN_MÅNED
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import java.time.YearMonth

fun TilkjentYtelse.tilSeparateTidslinjerForBarna(): Map<Aktør, Tidslinje<AndelTilkjentYtelse, Måned>> {

    return this.andelerTilkjentYtelse
        .filter { !it.erSøkersAndel() }
        .groupBy { it.aktør }
        .mapValues { (_, andeler) -> tidslinje { andeler.map { it.tilPeriode() } } }
}

fun Iterable<Tidslinje<AndelTilkjentYtelse, Måned>>.tilAndelerTilkjentYtelse(): List<AndelTilkjentYtelse> {
    return this.flatMap { it.tilAndelTilkjentYtelse() }
}

fun Tidslinje<AndelTilkjentYtelse, Måned>.tilAndelTilkjentYtelse(): List<AndelTilkjentYtelse> {
    return this
        .perioder().map {
            it.innhold?.medPeriode(it.fraOgMed.tilYearMonth(), it.tilOgMed.tilYearMonth())
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
        stønadFom = fraOgMed ?: MIN_MÅNED,
        stønadTom = tilOgMed ?: MAX_MÅNED
    )
