package no.nav.familie.ba.sak.kjerne.eøs.utbetaling

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.tilFamilieFellesTidslinjeForSøkersYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class UtbetalingTidslinjeService(
    private val beregningService: BeregningService,
) {
    fun hentUtbetalesIkkeOrdinærEllerUtvidetTidslinjer(
        behandlingId: BehandlingId,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    ): Map<Aktør, Tidslinje<Boolean>> {
        val barnasSkalIkkeUtbetalesTidslinjer =
            endretUtbetalingAndeler
                .tilBarnasSkalIkkeUtbetalesTidslinjer()

        val utvidetTidslinje =
            beregningService
                .hentAndelerTilkjentYtelseForBehandling(behandlingId.id)
                .tilFamilieFellesTidslinjeForSøkersYtelse(YtelseType.UTVIDET_BARNETRYGD)

        return barnasSkalIkkeUtbetalesTidslinjer
            .mapValues { (_, ordinærSkalIkkeUtbetalesTidslinje) ->
                val utbetalesIkkeOrdinærEllerUtvidetTidslinje =
                    ordinærSkalIkkeUtbetalesTidslinje
                        .kombinerMed(utvidetTidslinje) { ordinærSkalIkkeUtbetales, utvidetAndel ->
                            ordinærSkalIkkeUtbetales == true && (utvidetAndel == null || utvidetAndel.kalkulertUtbetalingsbeløp == 0)
                        }
                utbetalesIkkeOrdinærEllerUtvidetTidslinje
            }
    }
}

internal fun Iterable<EndretUtbetalingAndel>.tilBarnasSkalIkkeUtbetalesTidslinjer(): Map<Aktør, Tidslinje<Boolean>> =
    this
        .filter { it.årsak in listOf(Årsak.ETTERBETALING_3ÅR, Årsak.ETTERBETALING_3MND, Årsak.ALLEREDE_UTBETALT, Årsak.ENDRE_MOTTAKER) && it.prosent == BigDecimal.ZERO }
        .filter { it.person?.type == PersonType.BARN }
        .filter { it.person?.aktør != null }
        .groupBy { it.person?.aktør!! }
        .mapValues { (_, endringer) -> endringer.map { it.tilPeriode { true } } }
        .mapValues { (_, perioder) -> perioder.tilTidslinje() }

private fun <V> EndretUtbetalingAndel.tilPeriode(mapper: (EndretUtbetalingAndel) -> V) =
    Periode(
        fom = this.fom?.førsteDagIInneværendeMåned(),
        tom = this.tom?.sisteDagIInneværendeMåned(),
        verdi = mapper(this),
    )
