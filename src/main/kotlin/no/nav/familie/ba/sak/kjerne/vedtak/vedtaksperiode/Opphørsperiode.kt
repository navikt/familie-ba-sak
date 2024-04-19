package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje

fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilKombinertTidslinjePerAktørOgType(): Tidslinje<Collection<AndelTilkjentYtelseMedEndreteUtbetalinger>, Måned> {
    val andelTilkjentYtelsePerPersonOgType = groupBy { Pair(it.aktør, it.type) }

    val andelTilkjentYtelsePerPersonOgTypeTidslinjer =
        andelTilkjentYtelsePerPersonOgType.values.map { it.tilTidslinje() }

    return andelTilkjentYtelsePerPersonOgTypeTidslinjer.kombiner { it.toList() }
}

@JvmName("AndelTilkjentYtelseMedEndreteUtbetalingerTilTidslinje")
fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilTidslinje(): Tidslinje<AndelTilkjentYtelseMedEndreteUtbetalinger, Måned> =
    this.map {
        Periode(
            fraOgMed = it.stønadFom.tilTidspunkt(),
            tilOgMed = it.stønadTom.tilTidspunkt(),
            innhold = it,
        )
    }.tilTidslinje()
