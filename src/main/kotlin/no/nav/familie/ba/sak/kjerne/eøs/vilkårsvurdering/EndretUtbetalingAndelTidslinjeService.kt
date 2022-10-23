package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerSenereEnn
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerTidligereEnn
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import org.springframework.stereotype.Service

@Service
class EndretUtbetalingAndelTidslinjeService(
    val endretUtbetalingAndelService: EndretUtbetalingAndelService
) {
    fun hentBarnasHarEtterbetaling3ÅrTidslinjer(behandlingId: BehandlingId) =
        endretUtbetalingAndelService
            .hentForBehandling(behandlingId.id)
            .tilBarnasHarEtterbetaling3ÅrTidslinjer()
}

internal fun Iterable<EndretUtbetalingAndel>.tilBarnasHarEtterbetaling3ÅrTidslinjer(): Map<Aktør, Tidslinje<Boolean, Måned>> {
    return this
        .filter { it.årsak == Årsak.ETTERBETALING_3ÅR }
        .filter { it.person?.aktør != null }
        .groupBy { it.person?.aktør!! }
        .mapValues { (_, endringer) -> endringer.map { it.tilPeriode { true } } }
        .mapValues { (_, perioder) -> tidslinje { perioder } }
}

private fun <I> EndretUtbetalingAndel.tilPeriode(mapper: (EndretUtbetalingAndel) -> I?) = Periode(
    this.fom.tilTidspunktEllerTidligereEnn(this.tom),
    this.tom.tilTidspunktEllerSenereEnn(this.fom),
    mapper(this)
)
