package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.DagTidspunkt.Companion.tilTidspunktEllerSenereEnn
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.DagTidspunkt.Companion.tilTidspunktEllerTidligereEnn
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilFørsteDagIMåneden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilSisteDagIMåneden
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser

open class VedtaksperiodeMedBegrunnelserTidslinje(
    private val vedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>
) : Tidslinje<VedtaksperiodeMedBegrunnelser, Dag>() {

    override fun lagPerioder(): List<Periode<VedtaksperiodeMedBegrunnelser, Dag>> =
        vedtaksperioderMedBegrunnelser.map {
            Periode(
                fraOgMed = it.fom.tilTidspunktEllerTidligereEnn(it.tom),
                tilOgMed = it.tom.tilTidspunktEllerSenereEnn(it.fom),
                innhold = it
            )
        }
}

fun Tidslinje<VedtaksperiodeMedBegrunnelser, Dag>.lagVedtaksperioderMedBegrunnelser(): List<VedtaksperiodeMedBegrunnelser> =
    this.perioder().mapNotNull {
        it.innhold?.copy(
            fom = it.fraOgMed.tilFørsteDagIMåneden().tilLocalDateEllerNull(),
            tom = it.tilOgMed.tilSisteDagIMåneden().tilLocalDateEllerNull()
        )
    }
