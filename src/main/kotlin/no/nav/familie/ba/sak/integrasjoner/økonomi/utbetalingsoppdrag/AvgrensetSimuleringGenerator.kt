package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIUtbetalingUtil
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.felles.utbetalingsgenerator.Utbetalingsgenerator
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelDataLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import org.springframework.stereotype.Component
import java.time.YearMonth

@Component
class AvgrensetSimuleringGenerator(
    private val utbetalingsgenerator: Utbetalingsgenerator,
    private val behandlingsinformasjonUtleder: BehandlingsinformasjonUtleder,
    private val clockProvider: ClockProvider,
) {
    /*
    Genererer utbetalingsoppdrag for simulering fra tidligste endring eller 6 måneder tilbake i tid, avhengig av hva som kommer først.
    For å få til dette, avkorter vi andeler for inneværende og forrige behandling til denne datoen og sørger for at vi opphører alle kjeder fra første utbetaling før kjedene bygges opp på nytt.
    Dette vil resultere i en simulering som inneholder totalbeløpet per måned fra og med endringstidspunkt eller 6 måneder tilbake.
     */
    fun lagUtbetalingsoppdrag(
        saksbehandlerId: String,
        vedtak: Vedtak,
        tilkjentYtelse: TilkjentYtelse,
        forrigeTilkjentYtelse: TilkjentYtelse?,
        sisteAndelPerKjede: Map<IdentOgType, AndelDataLongId>,
    ): BeregnetUtbetalingsoppdragLongId {
        val behandlingsinformasjon =
            behandlingsinformasjonUtleder.utled(
                saksbehandlerId,
                vedtak,
                forrigeTilkjentYtelse,
                sisteAndelPerKjede,
                erSimulering = false,
            )

        val simulerFra =
            if (behandlingsinformasjon.opphørAlleKjederFra == null) {
                EndringIUtbetalingUtil
                    .utledEndringstidspunktForUtbetalingsbeløp(
                        nåværendeAndeler = tilkjentYtelse.andelerTilkjentYtelse.toList(),
                        forrigeAndeler = forrigeTilkjentYtelse?.andelerTilkjentYtelse?.toList() ?: emptyList(),
                    )?.let { førsteEndring ->
                        minOf(førsteEndring, YearMonth.now(clockProvider.get()).minusMonths(ANTALL_MÅNEDER_HISTORIKK))
                    }
            } else {
                null
            }

        val behandlingsinformasjonForOppdrag =
            if (simulerFra != null) {
                behandlingsinformasjon.copy(opphørKjederFraFørsteUtbetaling = true)
            } else {
                behandlingsinformasjon
            }

        return utbetalingsgenerator.lagUtbetalingsoppdrag(
            behandlingsinformasjon = behandlingsinformasjonForOppdrag,
            forrigeAndeler = forrigeTilkjentYtelse?.tilAndelData()?.avgrensFra(simulerFra) ?: emptyList(),
            nyeAndeler = tilkjentYtelse.tilAndelData().avgrensFra(simulerFra),
            sisteAndelPerKjede = sisteAndelPerKjede,
        )
    }

    private fun List<AndelDataLongId>.avgrensFra(fom: YearMonth?): List<AndelDataLongId> =
        if (fom == null) {
            this
        } else {
            filter { it.tom >= fom }.map { it.copy(fom = maxOf(it.fom, fom)) }
        }

    private companion object {
        const val ANTALL_MÅNEDER_HISTORIKK = 6L
    }
}
