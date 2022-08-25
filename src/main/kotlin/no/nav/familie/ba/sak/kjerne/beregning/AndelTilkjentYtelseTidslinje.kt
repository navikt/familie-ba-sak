package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunkt

class AndelTilkjentYtelseTidslinje(
    private val andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
) : Tidslinje<AndelTilkjentYtelseMedEndreteUtbetalinger, Måned>() {

    override fun lagPerioder(): List<Periode<AndelTilkjentYtelseMedEndreteUtbetalinger, Måned>> {
        return andelerTilkjentYtelse.map {
            Periode(
                fraOgMed = it.stønadFom.tilTidspunkt(),
                tilOgMed = it.stønadTom.tilTidspunkt(),
                innhold = it
            )
        }
    }
}
