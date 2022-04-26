package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeMedBegrunnelser

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning.KompetanseTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeMedBegrunnelserTidslinje
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.kombinerUtbetalingsperiodeMedKompetanse

fun slåSammenUtbetalingsperioderMedKompetanse(
    utbetalingsperioder: List<VedtaksperiodeMedBegrunnelser>,
    kompetanser: List<Kompetanse>
): List<VedtaksperiodeMedBegrunnelser> {

    val kompetanseTidslinje = KompetanseTidslinje(kompetanser)
    val utbetalingsTidslinje = VedtaksperiodeMedBegrunnelserTidslinje(utbetalingsperioder)

    val kombinertTidslinje =
        utbetalingsTidslinje.kombinerMed(kompetanseTidslinje, ::kombinerUtbetalingsperiodeMedKompetanse)

    return kombinertTidslinje.perioder().mapNotNull {
        it.innhold?.vedtaksperiodeMedBegrunnelser?.copy(
            fom = it.fraOgMed.tilLocalDate().førsteDagIInneværendeMåned(),
            tom = it.tilOgMed.tilLocalDate().sisteDagIMåned()
        )
    }
}
