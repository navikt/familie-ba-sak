package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeMedBegrunnelser

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning.KompetanseTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.snittKombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeMedBegrunnelserTidslinjeMåned

fun slåSammenUtbetalingsperioderMedKompetanse(
    utbetalingsperioder: List<VedtaksperiodeMedBegrunnelser>,
    kompetanser: List<Kompetanse>
): List<VedtaksperiodeMedBegrunnelser> {
    if (kompetanser.isEmpty()) return utbetalingsperioder

    val kompetanseTidslinje = KompetanseTidslinje(kompetanser)
    val utbetalingsTidslinje = VedtaksperiodeMedBegrunnelserTidslinjeMåned(utbetalingsperioder)

    val kombinertTidslinje =
        utbetalingsTidslinje
            .snittKombinerMed(kompetanseTidslinje) { vedtaksperiodeMedBegrunnelser, kompetanse ->
                vedtaksperiodeMedBegrunnelser?.let {
                    UtbetalingsperiodeMedOverlappendeKompetanse(
                        it,
                        kompetanse
                    )
                }
            }

    return kombinertTidslinje.lagVedtaksperioderMedBegrunnelser()
}

data class UtbetalingsperiodeMedOverlappendeKompetanse(
    val vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
    val kompetanse: Kompetanse?,
)

fun Tidslinje<UtbetalingsperiodeMedOverlappendeKompetanse, Måned>.lagVedtaksperioderMedBegrunnelser(): List<VedtaksperiodeMedBegrunnelser> =
    this.perioder().mapNotNull {
        it.innhold?.vedtaksperiodeMedBegrunnelser?.copy(
            fom = it.fraOgMed.tilLocalDate().førsteDagIInneværendeMåned(),
            tom = it.tilOgMed.tilLocalDate().sisteDagIMåned()
        )
    }
