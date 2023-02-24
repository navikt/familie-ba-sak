package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunktEllerSenereEnn
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunktEllerTidligereEnn
import org.springframework.stereotype.Service

@Service
class EndretUtbetalingAndelTidslinjeService(
    val endretUtbetalingAndelHentOgPersisterService: EndretUtbetalingAndelHentOgPersisterService
) {
    fun hentBarnasHarEtterbetaling3ÅrTidslinjer(behandlingId: BehandlingId) =
        endretUtbetalingAndelHentOgPersisterService
            .hentForBehandling(behandlingId.id)
            .tilBarnasHarEtterbetaling3ÅrTidslinjer()
}

internal fun Iterable<EndretUtbetalingAndel>.tilBarnasHarEtterbetaling3ÅrTidslinjer(): Map<Aktør, Tidslinje<Boolean, Måned>> {
    return this
        .filter { it.årsak == Årsak.ETTERBETALING_3ÅR }
        .filter { it.person?.type == PersonType.BARN }
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
