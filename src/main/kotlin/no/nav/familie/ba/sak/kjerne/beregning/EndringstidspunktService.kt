package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class EndringstidspunktService(
    private val behandlingRepository: BehandlingRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val kompetanseRepository: PeriodeOgBarnSkjemaRepository<Kompetanse>,
    private val featureToggleService: FeatureToggleService
) {
    fun finnEndringstidpunkForBehandling(behandlingId: Long): LocalDate {
        val nyBehandling = behandlingRepository.finnBehandling(behandlingId)

        val iverksatteBehandlinger = behandlingRepository.finnIverksatteBehandlinger(fagsakId = nyBehandling.fagsak.id)
        val sistIverksatteBehandling = Behandlingutils.hentSisteBehandlingSomErIverksatt(iverksatteBehandlinger)
            ?: return TIDENES_MORGEN

        val nyeAndelerTilkjentYtelse = andelTilkjentYtelseRepository
            .finnAndelerTilkjentYtelseForBehandling(behandlingId = behandlingId)

        val forrigeAndelerTilkjentYtelse = andelTilkjentYtelseRepository
            .finnAndelerTilkjentYtelseForBehandling(behandlingId = sistIverksatteBehandling.id)

        val førsteEndringstidspunktFraAndelTilkjentYtelse = nyeAndelerTilkjentYtelse.hentFørsteEndringstidspunkt(
            forrigeAndelerTilkjentYtelse = forrigeAndelerTilkjentYtelse
        ) ?: TIDENES_ENDE

        val førsteEndringstidspunktIKompetansePerioder =
            if (featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS)) {
                val kompetansePerioder = kompetanseRepository.findByBehandlingId(nyBehandling.id)
                val forrigeKompetansePerioder = kompetanseRepository.findByBehandlingId(sistIverksatteBehandling.id)
                kompetansePerioder.finnFørsteEndringstidspunkt(forrigeKompetansePerioder)
            } else TIDENES_ENDE

        return minOf(førsteEndringstidspunktFraAndelTilkjentYtelse, førsteEndringstidspunktIKompetansePerioder)
    }
}
