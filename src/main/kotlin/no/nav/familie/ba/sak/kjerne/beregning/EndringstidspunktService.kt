package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils.finnSisteAvsluttedeBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class EndringstidspunktService(
    private val behandlingRepository: BehandlingRepository,
    private val kompetanseRepository: PeriodeOgBarnSkjemaRepository<Kompetanse>,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService
) {
    fun finnEndringstidpunkForBehandling(behandlingId: Long): LocalDate {
        val nyBehandling = behandlingRepository.finnBehandling(behandlingId)

        val sistIverksatteBehandling =
            behandlingRepository.finnIverksatteBehandlinger(fagsakId = nyBehandling.fagsak.id)
                .finnSisteAvsluttedeBehandling()
                ?: return TIDENES_MORGEN

        val nyeAndelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId)

        val forrigeAndelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(sistIverksatteBehandling.id)

        val førsteEndringstidspunktFraAndelTilkjentYtelse = nyeAndelerTilkjentYtelse.hentFørsteEndringstidspunkt(
            forrigeAndelerTilkjentYtelse = forrigeAndelerTilkjentYtelse
        ) ?: TIDENES_ENDE

        val kompetansePerioder = kompetanseRepository.finnFraBehandlingId(nyBehandling.id)
        val forrigeKompetansePerioder = kompetanseRepository.finnFraBehandlingId(sistIverksatteBehandling.id)
        val førsteEndringstidspunkt = kompetansePerioder.finnFørsteEndringstidspunkt(forrigeKompetansePerioder)

        val førsteEndringstidspunktIKompetansePerioder =
            if (førsteEndringstidspunkt != TIDENES_ENDE.toYearMonth()) {
                førsteEndringstidspunkt.førsteDagIInneværendeMåned()
            } else {
                TIDENES_ENDE
            }

        return minOf(førsteEndringstidspunktFraAndelTilkjentYtelse, førsteEndringstidspunktIKompetansePerioder)
    }
}
