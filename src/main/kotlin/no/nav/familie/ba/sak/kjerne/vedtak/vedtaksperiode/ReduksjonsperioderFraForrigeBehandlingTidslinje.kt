package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt.Companion.tilTidspunktEllerSenereEnn
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt.Companion.tilTidspunktEllerTidligereEnn
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser

class ReduksjonsperioderFraForrigeBehandlingTidslinje(
    private val vedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>
) : VedtaksperiodeMedBegrunnelserTidslinje(vedtaksperioderMedBegrunnelser) {

    override fun lagPerioder(): List<Periode<VedtaksperiodeMedBegrunnelser, Dag>> =
        vedtaksperioderMedBegrunnelser.map {
            Periode(
                fraOgMed = it.fom.tilTidspunktEllerTidligereEnn(it.tom),
                tilOgMed = it.tom.tilTidspunktEllerSenereEnn(it.fom),
                innhold = it.copy(fom = null, tom = null) // Gjør at perioder med samme innhold blir slått sammen
            )
        }
}
