package no.nav.familie.ba.sak.kjerne.eøs.utbetaling

import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.tilTidslinjeForSøkersYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.tilBarnasSkalIkkeUtbetalesTidslinjer
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import org.springframework.stereotype.Service

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
        val utvidetTidslinje = beregningService.hentAndelerTilkjentYtelseForBehandling(behandlingId.id).tilTidslinjeForSøkersYtelse(YtelseType.UTVIDET_BARNETRYGD)
        return barnasSkalIkkeUtbetalesTidslinjer.mapValues { (_, ordinærSkalIkkeUtbetalesTidslinje) ->
            ordinærSkalIkkeUtbetalesTidslinje.kombinerMed(utvidetTidslinje) { ordinærSkalIkkeUtbetales, utvidetAndel ->
                ordinærSkalIkkeUtbetales == true && (utvidetAndel == null || utvidetAndel.kalkulertUtbetalingsbeløp == 0)
            }
        }
    }
}
