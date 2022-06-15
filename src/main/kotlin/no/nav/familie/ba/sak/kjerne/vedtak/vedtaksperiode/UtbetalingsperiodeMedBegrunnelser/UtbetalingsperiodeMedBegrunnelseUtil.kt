package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeMedBegrunnelser

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNullMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeMedBegrunnelserTidslinjeMåned

fun slåSammenUtbetalingsperioderMedKompetanse(
    utbetalingsperioder: List<VedtaksperiodeMedBegrunnelser>,
    kompetanser: List<Kompetanse>
): List<VedtaksperiodeMedBegrunnelser> {
    if (kompetanser.isEmpty()) return utbetalingsperioder

    val kompetanseTidslinjer = kompetanser.tilSeparateTidslinjerForBarna()
    val utbetalingsTidslinje = VedtaksperiodeMedBegrunnelserTidslinjeMåned(utbetalingsperioder)

    return kompetanseTidslinjer.values
        .kombinerUtenNull { it.toList() }
        .kombinerUtenNullMed(utbetalingsTidslinje) { kompetanser, vedtaksperiodeMedBegrunnelse ->
            UtbetalingsperiodeMedOverlappendeKompetanse(vedtaksperiodeMedBegrunnelse, kompetanser)
        }.lagVedtaksperioderMedBegrunnelser()
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
