package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeMedBegrunnelser

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeMedBegrunnelserTidslinjeMåned

fun slåSammenUtbetalingsperioderMedKompetanse(
    utbetalingsperioder: List<VedtaksperiodeMedBegrunnelser>,
    kompetanser: List<Kompetanse>
): List<VedtaksperiodeMedBegrunnelser> {
    if (kompetanser.isEmpty()) return utbetalingsperioder

    val kompetanseTidslinjer = kompetanser.tilSeparateTidslinjerForBarna()
    val utbetalingsTidslinje = VedtaksperiodeMedBegrunnelserTidslinjeMåned(utbetalingsperioder)

    val initiellKombinertTidslinje = utbetalingsTidslinje.map {
        it?.let {
            UtbetalingsperiodeMedOverlappendeKompetanse(
                it,
                emptyList()
            )
        }
    }

    val kombinertTidslinje = kompetanseTidslinjer.values.fold(
        initiellKombinertTidslinje
    ) { acc, kompetanseTidslinje ->
        acc.kombinerMed(kompetanseTidslinje) { utbetalingsperiodeMedOverlappendeKompetanse, kompetanse ->
            utbetalingsperiodeMedOverlappendeKompetanse?.let {
                if (kompetanse == null) {
                    utbetalingsperiodeMedOverlappendeKompetanse
                } else {
                    UtbetalingsperiodeMedOverlappendeKompetanse(
                        utbetalingsperiodeMedOverlappendeKompetanse.vedtaksperiodeMedBegrunnelser,
                        utbetalingsperiodeMedOverlappendeKompetanse.kompetanser + kompetanse
                    )
                }
            }
        }
    }

    return kombinertTidslinje.lagVedtaksperioderMedBegrunnelser()
}

data class UtbetalingsperiodeMedOverlappendeKompetanse(
    val vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
    val kompetanser: List<Kompetanse>,
)

fun Tidslinje<UtbetalingsperiodeMedOverlappendeKompetanse, Måned>.lagVedtaksperioderMedBegrunnelser(): List<VedtaksperiodeMedBegrunnelser> =
    this.perioder().mapNotNull {
        it.innhold?.vedtaksperiodeMedBegrunnelser?.copy(
            fom = it.fraOgMed.tilLocalDateEllerNull()?.førsteDagIInneværendeMåned(),
            tom = it.tilOgMed.tilLocalDateEllerNull()?.sisteDagIMåned()
        )
    }
