package no.nav.familie.ba.sak.kjerne.eøs.utbetaling

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.tilTidslinjeForSøkersYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.skalUtbetales
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.leftJoin
import org.springframework.stereotype.Service

@Service
class UtbetalingTidslinjeService(
    private val beregningService: BeregningService,
) {
    fun hentEndredeUtbetalingsPerioderSomKreverKompetanseTidslinjer(
        behandlingId: BehandlingId,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    ): Map<Aktør, Tidslinje<Boolean>> {
        val utbetalesIkkeOrdinærEllerUtvidetTidslinjer =
            hentUtbetalesIkkeOrdinærEllerUtvidetTidslinjer(
                behandlingId = behandlingId,
                endretUtbetalingAndeler = endretUtbetalingAndeler,
            )

        val utbetalesIkkeOrdinærMenUtvidetOgKompetanseKrevesTidslinjer =
            hentUtbetalesIkkeOrdinærMenUtvidetTidslinjer(
                behandlingId = behandlingId,
                endretUtbetalingAndeler = endretUtbetalingAndeler,
            )

        return utbetalesIkkeOrdinærEllerUtvidetTidslinjer
            .leftJoin(utbetalesIkkeOrdinærMenUtvidetOgKompetanseKrevesTidslinjer) { ingenUtbetalingOrdinærEllerUtvidet, ingenUtbetalingOrdinærMenUtvidetOgKreverKompetanse ->
                when (ingenUtbetalingOrdinærEllerUtvidet) {
                    true -> false // Ingen utbetaling av ordinær eller utvidet og kompetanse kreves ikke
                    false -> ingenUtbetalingOrdinærMenUtvidetOgKreverKompetanse // Krever kompetanse i noen tilfeller dersom ingen utbetaling av ordinær men utbetaling av utvidet
                    null -> null
                }
            }.mapValues { it.value.filtrerIkkeNull() }
    }

    fun hentUtbetalesIkkeOrdinærEllerUtvidetTidslinjer(
        behandlingId: BehandlingId,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    ): Map<Aktør, Tidslinje<Boolean>> {
        val barnasEndretUtbetalingSkalIkkeUtbetalesTidslinjer =
            endretUtbetalingAndeler
                .tilBarnasEndretUtbetalingSkalIkkeUtbetalesTidslinjer()

        val utvidetTidslinje =
            beregningService
                .hentAndelerTilkjentYtelseForBehandling(behandlingId.id)
                .tilTidslinjeForSøkersYtelse(YtelseType.UTVIDET_BARNETRYGD)

        return barnasEndretUtbetalingSkalIkkeUtbetalesTidslinjer
            .mapValues { (_, endretUtbetalingSkalIkkeUtbetalesTidslinje) ->
                endretUtbetalingSkalIkkeUtbetalesTidslinje
                    .kombinerMed(utvidetTidslinje) { endretUtbetalingAndelSkalIkkeUtbetales, utvidetAndel ->
                        endretUtbetalingAndelSkalIkkeUtbetales?.let {
                            erIngenUtbetalingAvUtvidet(
                                utvidetAndel,
                            )
                        }
                    }
            }
    }

    private fun hentUtbetalesIkkeOrdinærMenUtvidetTidslinjer(
        behandlingId: BehandlingId,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    ): Map<Aktør, Tidslinje<Boolean>> {
        val barnasEndretUtbetalingSkalIkkeUtbetalesTidslinjer =
            endretUtbetalingAndeler
                .tilBarnasEndretUtbetalingSkalIkkeUtbetalesTidslinjer()

        val utvidetTidslinje =
            beregningService
                .hentAndelerTilkjentYtelseForBehandling(behandlingId.id)
                .tilTidslinjeForSøkersYtelse(YtelseType.UTVIDET_BARNETRYGD)

        return barnasEndretUtbetalingSkalIkkeUtbetalesTidslinjer
            .mapValues { (_, endretUtbetalingSkalIkkeUtbetalesTidslinje) ->
                val utbetalesIkkeOrdinærEllerUtvidetTidslinje =
                    endretUtbetalingSkalIkkeUtbetalesTidslinje
                        .kombinerMed(utvidetTidslinje) { endretUtbetalingAndelSkalIkkeUtbetales, utvidetAndel ->
                            endretUtbetalingAndelSkalIkkeUtbetales?.let { erUtbetalingAvUtvidetOgKompetanseKreves(endretUtbetalingAndelSkalIkkeUtbetales, utvidetAndel) }
                        }
                utbetalesIkkeOrdinærEllerUtvidetTidslinje
            }
    }
}

internal fun Iterable<EndretUtbetalingAndel>.tilBarnasEndretUtbetalingSkalIkkeUtbetalesTidslinjer(): Map<Aktør, Tidslinje<EndretUtbetalingAndel>> =
    this
        .filter { !it.skalUtbetales() }
        .filter { it.person?.type == PersonType.BARN }
        .filter { it.person?.aktør != null }
        .groupBy { it.person?.aktør!! }
        .mapValues { (_, endringer) -> endringer.map { endretUtbetalingAndel -> endretUtbetalingAndel.tilPeriode { it } } }
        .mapValues { (_, perioder) -> perioder.tilTidslinje() }

private fun <V> EndretUtbetalingAndel.tilPeriode(mapper: (EndretUtbetalingAndel) -> V) =
    Periode(
        fom = this.fom?.førsteDagIInneværendeMåned(),
        tom = this.tom?.sisteDagIInneværendeMåned(),
        verdi = mapper(this),
    )

private fun erIngenUtbetalingAvUtvidet(
    utvidetAndel: AndelTilkjentYtelse?,
) = (utvidetAndel == null || utvidetAndel.kalkulertUtbetalingsbeløp == 0)

private fun erUtbetalingAvUtvidetOgKompetanseKreves(
    endretUtbetalingAndel: EndretUtbetalingAndel,
    utvidetAndel: AndelTilkjentYtelse?,
): Boolean =
    utvidetAndel != null &&
        utvidetAndel.kalkulertUtbetalingsbeløp != 0 &&
        endretUtbetalingAndel.årsak?.kreverKompetanseVedIngenUtbetalingOgOverlappendeUtvidetBarnetrygd() == true
