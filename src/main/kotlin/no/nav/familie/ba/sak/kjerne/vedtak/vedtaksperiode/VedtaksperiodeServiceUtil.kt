package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser

fun hentEndredeUtbetalingsperioderMedBegrunnelser(
    vedtak: Vedtak,
    endredeUtbetalingsAndeler: List<EndretUtbetalingAndel>
): List<VedtaksperiodeMedBegrunnelser> {

    val periodegrupperteEndringsperioder: Map<NullablePeriode, List<EndretUtbetalingAndel>> =
        endredeUtbetalingsAndeler.groupBy {
            NullablePeriode(
                it.fom?.førsteDagIInneværendeMåned(),
                it.tom?.sisteDagIInneværendeMåned()
            )
        }

    return periodegrupperteEndringsperioder
        .map { (periode, endredePerioder) ->
            endredePerioder.tilVedtaksperiodeMedBegrunnelser(vedtak, periode.fom, periode.tom)
        }
}
