package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
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
        val gammelBehandling = Behandlingutils.hentSisteBehandlingSomErIverksatt(iverksatteBehandlinger)
            ?: return TIDENES_MORGEN

        val nyeAndelerTilkjentYtelse = andelTilkjentYtelseRepository
            .finnAndelerTilkjentYtelseForBehandling(behandlingId = behandlingId)

        val gamleAndelerTilkjentYtelse = andelTilkjentYtelseRepository
            .finnAndelerTilkjentYtelseForBehandling(behandlingId = gammelBehandling.id)

        val perioderMedEndringer = nyeAndelerTilkjentYtelse.hentPerioderMedEndringerFra(
            forrigeAndelerTilkjentYtelse = gamleAndelerTilkjentYtelse
        )

        return perioderMedEndringer.minOfOrNull { it.fom } ?: TIDENES_ENDE
    }
}
