package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.PRAKTISK_SENESTE_DAG
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.PRAKTISK_TIDLIGSTE_DAG
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import java.time.YearMonth

class VedtaksperiodeMedBegrunnelserTidslinje(
    private val vedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>,
) : Tidslinje<VedtaksperiodeMedBegrunnelser, Måned>() {

    override fun fraOgMed(): Tidspunkt<Måned> = vedtaksperioderMedBegrunnelser.minOf {
        it.fom?.toYearMonth().tilTidspunktEllerUendeligLengeSiden { PRAKTISK_TIDLIGSTE_DAG.toYearMonth() }
    }

    override fun tilOgMed(): Tidspunkt<Måned> = vedtaksperioderMedBegrunnelser.minOf {
        it.fom?.toYearMonth().tilTidspunktEllerUendeligLengeTil { PRAKTISK_SENESTE_DAG.toYearMonth() }
    }

    override fun lagPerioder(): List<Periode<VedtaksperiodeMedBegrunnelser, Måned>> =
        vedtaksperioderMedBegrunnelser.map { it.tilPeriode() }

    private fun VedtaksperiodeMedBegrunnelser.tilPeriode(): Periode<VedtaksperiodeMedBegrunnelser, Måned> {
        val fom = this.fom?.toYearMonth()
        val tom = this.tom?.toYearMonth()

        return Periode(
            fraOgMed = fom.tilTidspunktEllerUendeligLengeSiden { fom ?: YearMonth.now() },
            tilOgMed = tom.tilTidspunktEllerUendeligLengeTil { tom ?: YearMonth.now() },
            innhold = this
        )
    }
}

data class UtbetalingsperiodeMedOverlappendeKompetanse(
    val vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
    val kompetanse: Kompetanse?,
)

fun kombinerUtbetalingsperiodeMedKompetanse(
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser?,
    kompetanse: Kompetanse?,
): UtbetalingsperiodeMedOverlappendeKompetanse? = when {
    vedtaksperiodeMedBegrunnelser == null && kompetanse == null -> null
    vedtaksperiodeMedBegrunnelser == null -> throw Feil("Kan ikke ha kompetanse uten utbetalingsperiode")
    else -> UtbetalingsperiodeMedOverlappendeKompetanse(vedtaksperiodeMedBegrunnelser, kompetanse)
}
