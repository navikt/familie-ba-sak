package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatOpphørUtils.filtrerBortIrrelevanteAndeler
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class StarttidspunktForUtbetalingstabellService(
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) {
    fun finnStarttidspunktForUtbetalingstabell(behandling: Behandling): LocalDate {
        val førsteJanuarIFjor = LocalDate.now().minusYears(1).withDayOfYear(1)
        val endringstidspunkt = vedtaksperiodeService.finnEndringstidspunktForBehandling(behandling.id)

        return when {
            behandling.opprettetÅrsak != BehandlingÅrsak.ÅRLIG_KONTROLL || endringstidspunkt.isBefore(førsteJanuarIFjor) -> endringstidspunkt
            else -> {
                val endretutbetalingAndeler = endretUtbetalingAndelRepository.findByBehandlingId(behandlingId = behandling.id)
                val tidligsteUtbetaling =
                    andelTilkjentYtelseRepository
                        .finnAndelerTilkjentYtelseForBehandling(behandling.id)
                        .filtrerBortIrrelevanteAndeler(endretutbetalingAndeler)
                        .minOfOrNull { it.stønadFom }
                        ?.toLocalDate() ?: return TIDENES_ENDE

                tidligsteUtbetaling.coerceAtLeast(førsteJanuarIFjor)
            }
        }
    }
}
