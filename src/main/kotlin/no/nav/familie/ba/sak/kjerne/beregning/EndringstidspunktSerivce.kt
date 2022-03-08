package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class EndringstidspunktSerivce(
    private val behandlingRepository: BehandlingRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
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

        val førsteEndringstidspunkt = nyeAndelerTilkjentYtelse.hentFørsteEndringstidspunkt(
            forrigeAndelerTilkjentYtelse = forrigeAndelerTilkjentYtelse
        ) ?: TIDENES_ENDE

        LoggerFactory.getLogger("secureLogger")
            .info("Første endringstidspunkt: $førsteEndringstidspunkt.\nForrige andeler: $forrigeAndelerTilkjentYtelse, nye andeler: $nyeAndelerTilkjentYtelse")
        return førsteEndringstidspunkt
    }
}
