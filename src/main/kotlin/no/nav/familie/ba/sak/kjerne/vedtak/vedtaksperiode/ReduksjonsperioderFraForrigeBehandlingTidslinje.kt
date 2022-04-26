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
        vedtaksperioderMedBegrunnelser.map {
            Periode(
                fraOgMed = it.fom.tilTidspunktEllerDefault { PRAKTISK_TIDLIGSTE_DAG },
                tilOgMed = it.tom.tilTidspunktEllerDefault { PRAKTISK_SENESTE_DAG },
                innhold = it.copy(fom = null, tom = null) // Gjør at perioder med samme innhold blir slått sammen
            )
        }
}
