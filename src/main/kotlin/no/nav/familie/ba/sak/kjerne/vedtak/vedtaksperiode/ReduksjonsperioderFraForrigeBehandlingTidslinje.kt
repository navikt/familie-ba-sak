package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.PRAKTISK_SENESTE_DAG
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.PRAKTISK_TIDLIGSTE_DAG
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilTidspunktEllerDefault
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser

class ReduksjonsperioderFraForrigeBehandlingTidslinje(
    private val vedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>,
) : VedtaksperiodeMedBegrunnelserTidslinje(vedtaksperioderMedBegrunnelser) {

    override fun lagPerioder(): List<Periode<VedtaksperiodeMedBegrunnelser, Dag>> =
        vedtaksperioderMedBegrunnelser.map { it.tilPeriode() }

    private fun VedtaksperiodeMedBegrunnelser.tilPeriode(): Periode<VedtaksperiodeMedBegrunnelser, Dag> {
        val fom = this.fom
        val tom = this.tom

        return Periode(
            fraOgMed = fom.tilTidspunktEllerDefault { PRAKTISK_TIDLIGSTE_DAG },
            tilOgMed = tom.tilTidspunktEllerDefault { PRAKTISK_SENESTE_DAG },
            innhold = this.copy(fom = null, tom = null) // Gjør at perioder med samme innhold blir slått sammen
        )
    }
}
