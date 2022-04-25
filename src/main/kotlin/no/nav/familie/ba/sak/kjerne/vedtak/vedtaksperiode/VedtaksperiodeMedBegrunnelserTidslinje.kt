package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.PRAKTISK_SENESTE_DAG
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.PRAKTISK_TIDLIGSTE_DAG
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilTidspunktEllerDefault
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser

open class VedtaksperiodeMedBegrunnelserTidslinje(
    private val vedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>,
) : Tidslinje<VedtaksperiodeMedBegrunnelser, Dag>() {

    override fun fraOgMed(): Tidspunkt<Dag> = vedtaksperioderMedBegrunnelser.minOf {
        it.fom.tilTidspunktEllerDefault { PRAKTISK_TIDLIGSTE_DAG }
    }

    override fun tilOgMed(): Tidspunkt<Dag> = vedtaksperioderMedBegrunnelser.minOf {
        it.tom.tilTidspunktEllerDefault { PRAKTISK_SENESTE_DAG }
    }

    override fun lagPerioder(): List<Periode<VedtaksperiodeMedBegrunnelser, Dag>> =
        vedtaksperioderMedBegrunnelser.map { it.tilPeriode() }

    private fun VedtaksperiodeMedBegrunnelser.tilPeriode(): Periode<VedtaksperiodeMedBegrunnelser, Dag> {
        val fom = this.fom
        val tom = this.tom

        return Periode(
            fraOgMed = fom.tilTidspunktEllerDefault { PRAKTISK_TIDLIGSTE_DAG },
            tilOgMed = tom.tilTidspunktEllerDefault { PRAKTISK_SENESTE_DAG },
            innhold = this
        )
    }
}

fun Tidslinje<VedtaksperiodeMedBegrunnelser, Dag>.lagVedtaksperioderMedBegrunnelser(): List<VedtaksperiodeMedBegrunnelser> =
    this.perioder().mapNotNull { it.innhold?.copy(fom = it.fraOgMed.tilLocalDate(), tom = it.tilOgMed.tilLocalDate()) }

class UtbetalingsperiodeOgReduksjonsperiodeKombinator {
    fun kombiner(periode1: VedtaksperiodeMedBegrunnelser?, periode2: VedtaksperiodeMedBegrunnelser?): VedtaksperiodeMedBegrunnelser? {
        return when {
            periode1 == null && periode2 == null -> null
            periode2 != null -> periode2
            else -> periode1
        }
    }
}
