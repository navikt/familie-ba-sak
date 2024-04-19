package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalingerTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned

fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilKombinertTidslinjePerAktørOgType(): Tidslinje<Collection<AndelTilkjentYtelseMedEndreteUtbetalinger>, Måned> {
    val andelTilkjentYtelsePerPersonOgType = groupBy { Pair(it.aktør, it.type) }

    val andelTilkjentYtelsePerPersonOgTypeTidslinjer =
        andelTilkjentYtelsePerPersonOgType.values.map { AndelTilkjentYtelseMedEndreteUtbetalingerTidslinje(it) }

    return andelTilkjentYtelsePerPersonOgTypeTidslinjer.kombiner { it.toList() }
}
