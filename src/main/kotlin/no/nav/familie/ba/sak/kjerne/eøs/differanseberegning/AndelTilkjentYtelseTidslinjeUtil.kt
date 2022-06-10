package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseDomene
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje

fun TilkjentYtelse.tilSeparateTidslinjerForBarna(): Map<Aktør, Tidslinje<AndelTilkjentYtelse, Måned>> {

    return this.andelerTilkjentYtelse
        .filter { !it.erSøkersAndel() }
        .groupBy { it.aktør }
        .mapValues { (_, andeler) -> tidslinje { andeler.map { it.tilPeriode() } } }
}

fun <T : AndelTilkjentYtelseDomene<T>> Tidslinje<T, Måned>.tilAndelTilkjentYtelse(): List<T> {
    return this
        .perioder().map {
            it.innhold?.medPeriode(it.fraOgMed.tilYearMonth(), it.tilOgMed.tilYearMonth())
        }.filterNotNull()
}

fun AndelTilkjentYtelse.tilPeriode() = Periode(
    this.stønadFom.tilTidspunkt(),
    this.stønadTom.tilTidspunkt(),
    // Ta bort periode, slik at det ikke blir med på innholdet som vurderes for likhet
    this.utenPeriode()
)
