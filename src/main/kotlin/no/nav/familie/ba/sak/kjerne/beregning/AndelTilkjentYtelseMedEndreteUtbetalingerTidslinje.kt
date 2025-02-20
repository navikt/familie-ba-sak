package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.Periode as FamilieFellesPeriode
import no.nav.familie.tidslinje.Tidslinje as FamilieFellesTidslinje

class AndelTilkjentYtelseMedEndreteUtbetalingerTidslinje(
    private val andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
) : Tidslinje<AndelTilkjentYtelseMedEndreteUtbetalinger, Måned>() {
    override fun lagPerioder(): List<Periode<AndelTilkjentYtelseMedEndreteUtbetalinger, Måned>> =
        andelerTilkjentYtelse.map {
            Periode(
                fraOgMed = it.stønadFom.tilTidspunkt(),
                tilOgMed = it.stønadTom.tilTidspunkt(),
                innhold = it,
            )
        }
}

fun Collection<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilTidslinje(): FamilieFellesTidslinje<AndelTilkjentYtelseMedEndreteUtbetalinger> =
    this
        .map {
            FamilieFellesPeriode(
                verdi = it,
                fom = it.stønadFom.førsteDagIInneværendeMåned(),
                tom = it.stønadTom.sisteDagIInneværendeMåned(),
            )
        }.tilTidslinje()
