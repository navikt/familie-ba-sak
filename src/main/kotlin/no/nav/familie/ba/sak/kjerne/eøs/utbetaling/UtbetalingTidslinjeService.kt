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
        // Boolsk tidslinje per barn som er true i alle perioder hvor ordinær barnetrygd ikke skal utbetales for barnet samtidig som søker ikke får utbetalt utvidet barnetrygd
        val ingenUtbetalingAvOrdinærBarnetrygdForBarnEllerUtvidetBarnetrygdForSøkerTidslinjePerBarn: Map<Aktør, Tidslinje<Boolean>> =
            hentIngenUtbetalingAvOrdinærBarnetrygdForBarnEllerUtvidetBarnetrygdForSøkerTidslinjePerBarn(
                behandlingId = behandlingId,
                endretUtbetalingAndeler = endretUtbetalingAndeler,
            )

        // Boolsk tidslinje per barn som er true i alle perioder hvor ordinær barnetrygd ikke skal utbetales for barnet samtidig som søker får utbetalt utvidet barnetrygd, og årsaken til endret utbetaling krever kompetanse
        val ingenUtbetalingAvOrdinærBarentrygdForBarnMenUtvidetBarnetrygdForSøkerOgKreverKompetanseTidslinjePerBarn: Map<Aktør, Tidslinje<Boolean>> =
            hentIngenUtbetalingAvOrdinærBarentrygdForBarnMenUtvidetBarnetrygdForSøkerOgKompetanseKrevesTidslinjePerBarn(
                behandlingId = behandlingId,
                endretUtbetalingAndeler = endretUtbetalingAndeler,
            )

        return ingenUtbetalingAvOrdinærBarnetrygdForBarnEllerUtvidetBarnetrygdForSøkerTidslinjePerBarn
            .leftJoin(ingenUtbetalingAvOrdinærBarentrygdForBarnMenUtvidetBarnetrygdForSøkerOgKreverKompetanseTidslinjePerBarn) { ingenUtbetalingOrdinærEllerUtvidet, ingenUtbetalingOrdinærMenUtvidetOgKreverKompetanse ->
                when (ingenUtbetalingOrdinærEllerUtvidet) {
                    true -> false

                    // Ingen utbetaling av ordinær eller utvidet og kompetanse kreves ikke
                    false -> ingenUtbetalingOrdinærMenUtvidetOgKreverKompetanse

                    // Krever kompetanse i noen tilfeller dersom ingen utbetaling av ordinær men utbetaling av utvidet
                    null -> null
                }
            }.mapValues { it.value.filtrerIkkeNull() }
    }

    fun hentIngenUtbetalingAvOrdinærBarnetrygdForBarnEllerUtvidetBarnetrygdForSøkerTidslinjePerBarn(
        behandlingId: BehandlingId,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    ): Map<Aktør, Tidslinje<Boolean>> {
        val endretUtbetalingSkalIkkeUtbetalesTidslinjePerBarn =
            endretUtbetalingAndeler
                .tilBarnasEndretUtbetalingSkalIkkeUtbetalesTidslinjer()

        val utvidetBarnetrygdTidslinje =
            beregningService
                .hentAndelerTilkjentYtelseForBehandling(behandlingId.id)
                .tilTidslinjeForSøkersYtelse(YtelseType.UTVIDET_BARNETRYGD)

        return endretUtbetalingSkalIkkeUtbetalesTidslinjePerBarn
            .mapValues { (_, endretUtbetalingSkalIkkeUtbetalesTidslinje) ->
                endretUtbetalingSkalIkkeUtbetalesTidslinje
                    .kombinerMed(utvidetBarnetrygdTidslinje) { endretUtbetalingAndelSkalIkkeUtbetales, utvidetBarnetrygdAndel ->
                        endretUtbetalingAndelSkalIkkeUtbetales?.let {
                            !utvidetBarnetrygdAndel.skalUtbetales()
                        }
                    }
            }
    }

    private fun hentIngenUtbetalingAvOrdinærBarentrygdForBarnMenUtvidetBarnetrygdForSøkerOgKompetanseKrevesTidslinjePerBarn(
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
                            endretUtbetalingAndelSkalIkkeUtbetales?.let { utvidetBarnetrygdSkalUtbetalesOgKompetanseKreves(endretUtbetalingAndelSkalIkkeUtbetales, utvidetAndel) }
                        }
                utbetalesIkkeOrdinærEllerUtvidetTidslinje
            }
    }
}

internal fun Iterable<EndretUtbetalingAndel>.tilBarnasEndretUtbetalingSkalIkkeUtbetalesTidslinjer(): Map<Aktør, Tidslinje<EndretUtbetalingAndel>> =
    this
        .filter { !it.skalUtbetales() }
        .flatMap { andel ->
            andel.personer
                .filter { it.type == PersonType.BARN }
                .map { it.aktør to andel }
        }.groupBy({ it.first }, { it.second })
        .mapValues { (_, endringer) ->
            endringer
                .map { it.tilPeriode() }
                .tilTidslinje()
        }

private fun EndretUtbetalingAndel.tilPeriode() =
    Periode(
        fom = this.fom?.førsteDagIInneværendeMåned(),
        tom = this.tom?.sisteDagIInneværendeMåned(),
        verdi = this,
    )

private fun AndelTilkjentYtelse?.skalUtbetales() = (this != null && this.kalkulertUtbetalingsbeløp != 0)

private fun utvidetBarnetrygdSkalUtbetalesOgKompetanseKreves(
    endretUtbetalingAndel: EndretUtbetalingAndel,
    utvidetBarnetrygdAndel: AndelTilkjentYtelse?,
): Boolean =
    utvidetBarnetrygdAndel.skalUtbetales() &&
        endretUtbetalingAndel.årsak?.kreverKompetanseVedIngenUtbetalingOgOverlappendeUtvidetBarnetrygd() == true
