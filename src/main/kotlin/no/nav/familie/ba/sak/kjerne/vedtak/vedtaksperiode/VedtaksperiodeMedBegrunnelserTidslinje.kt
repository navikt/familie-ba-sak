package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt.Companion.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt.Companion.tilTidspunktEllerUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser

open class VedtaksperiodeMedBegrunnelserTidslinje(
    private val vedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>,
) : Tidslinje<VedtaksperiodeMedBegrunnelser, Dag>() {

    override fun fraOgMed(): Tidspunkt<Dag> = vedtaksperioderMedBegrunnelser.minOf {
        it.fom.tilTidspunktEllerUendeligLengeSiden()
    }

    override fun tilOgMed(): Tidspunkt<Dag> = vedtaksperioderMedBegrunnelser.minOf {
        it.tom.tilTidspunktEllerUendeligLengeTil()
    }

    override fun lagPerioder(): List<Periode<VedtaksperiodeMedBegrunnelser, Dag>> =
        vedtaksperioderMedBegrunnelser.map {
            Periode(
                fraOgMed = it.fom.tilTidspunktEllerUendeligLengeSiden(),
                tilOgMed = it.tom.tilTidspunktEllerUendeligLengeTil(),
                innhold = it
            )
        }
}

fun Tidslinje<VedtaksperiodeMedBegrunnelser, Dag>.lagVedtaksperioderMedBegrunnelser(): List<VedtaksperiodeMedBegrunnelser> =
    this.perioder().mapNotNull { it.innhold?.copy(fom = it.fraOgMed.tilLocalDateEllerNull(), tom = it.tilOgMed.tilLocalDateEllerNull()) }

fun kombinerUtbetalingsperiodeOgReduksjonsperiode(utbetalingsperiode: VedtaksperiodeMedBegrunnelser?, reduksjonsperiode: VedtaksperiodeMedBegrunnelser?): VedtaksperiodeMedBegrunnelser? {
    return when {
        utbetalingsperiode == null && reduksjonsperiode == null -> null
        reduksjonsperiode != null -> reduksjonsperiode // hvis reduksjonsperiode finnes skal vi bruke innhold fra den
        else -> utbetalingsperiode
    }
}
