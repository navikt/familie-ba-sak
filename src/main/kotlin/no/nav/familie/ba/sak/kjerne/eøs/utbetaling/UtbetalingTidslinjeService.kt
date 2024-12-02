package no.nav.familie.ba.sak.kjerne.eøs.utbetaling

import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.tilTidslinjeForSøkersYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunktEllerUendeligSent
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunktEllerUendeligTidlig
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class UtbetalingTidslinjeService(
    private val beregningService: BeregningService,
) {
    fun hentUtbetalesIkkeOrdinærEllerUtvidetTidslinjer(
        behandlingId: BehandlingId,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    ): Map<Aktør, Tidslinje<Boolean, Måned>> {
        val barnasSkalIkkeUtbetalesTidslinjer =
            endretUtbetalingAndeler
                .tilBarnasSkalIkkeUtbetalesTidslinjer()

        val utvidetTidslinje =
            beregningService
                .hentAndelerTilkjentYtelseForBehandling(behandlingId.id)
                .tilTidslinjeForSøkersYtelse(YtelseType.UTVIDET_BARNETRYGD)

        return barnasSkalIkkeUtbetalesTidslinjer
            .mapValues { (_, ordinærSkalIkkeUtbetalesTidslinje) ->
                val utbetalesIkkeOrdinærEllerUtvidetTidslinje =
                    ordinærSkalIkkeUtbetalesTidslinje.kombinerMed(utvidetTidslinje) { ordinærSkalIkkeUtbetales, utvidetAndel ->
                        ordinærSkalIkkeUtbetales == true && (utvidetAndel == null || utvidetAndel.kalkulertUtbetalingsbeløp == 0)
                    }
                utbetalesIkkeOrdinærEllerUtvidetTidslinje
            }
    }
}

internal fun Iterable<EndretUtbetalingAndel>.tilBarnasSkalIkkeUtbetalesTidslinjer(): Map<Aktør, Tidslinje<Boolean, Måned>> =
    this
        .filter { it.årsak in listOf(Årsak.ETTERBETALING_3ÅR, Årsak.ETTERBETALING_3MND, Årsak.ALLEREDE_UTBETALT, Årsak.ENDRE_MOTTAKER) && it.prosent == BigDecimal.ZERO }
        .filter { it.person?.type == PersonType.BARN }
        .filter { it.person?.aktør != null }
        .groupBy { it.person?.aktør!! }
        .mapValues { (_, endringer) -> endringer.map { it.tilPeriode { true } } }
        .mapValues { (_, perioder) -> tidslinje { perioder } }

private fun <I> EndretUtbetalingAndel.tilPeriode(mapper: (EndretUtbetalingAndel) -> I?) =
    Periode(
        fraOgMed = this.fom.tilTidspunktEllerUendeligTidlig(tom),
        tilOgMed = this.tom.tilTidspunktEllerUendeligSent(fom),
        innhold = mapper(this),
    )
