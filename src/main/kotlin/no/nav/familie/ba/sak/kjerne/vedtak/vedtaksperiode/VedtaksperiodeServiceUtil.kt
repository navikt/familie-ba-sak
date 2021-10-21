package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser

// TODO: Slett og fjern fra tester
fun hentEndredeUtbetalingsperioderMedBegrunnelser(
    vedtak: Vedtak,
    endredeUtbetalingsAndeler: List<EndretUtbetalingAndel>,
): List<VedtaksperiodeMedBegrunnelser> {

    val periodegrupperteEndringsperioder: Map<NullablePeriode, List<AndelTilkjentYtelse>> =
        endredeUtbetalingsAndeler.flatMap { it.andelTilkjentYtelser }.groupBy {
            NullablePeriode(
                it.stønadFom.førsteDagIInneværendeMåned(),
                it.stønadTom.sisteDagIInneværendeMåned()
            )
        }

    return periodegrupperteEndringsperioder
        .map { (periode, andeler) ->
            andeler.single().endretUtbetalingAndeler.tilVedtaksperiodeMedBegrunnelser(vedtak, periode.fom, periode.tom)
        }
}
