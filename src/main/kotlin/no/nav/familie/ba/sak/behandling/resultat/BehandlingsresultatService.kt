package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.behandling.resultat.BehandlingsresultatUtils
import no.nav.familie.ba.sak.behandling.resultat.Krav
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import org.springframework.stereotype.Service
import java.util.*

@Service
class BehandlingsresultatService(
        private val behandlingService: BehandlingService,
        private val søknadGrunnlagService: SøknadGrunnlagService,
        private val beregningService: BeregningService
) {

    fun utledBehandlingsresultat(behandlingId: Long): List<Krav> {
        val behandling = behandlingService.hent(behandlingId = behandlingId)
        val forrigeBehandling = behandlingService.hentForrigeBehandlingSomErIverksatt(behandling)

        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandlingId)
        val forrigeTilkjentYtelse: TilkjentYtelse? =
                if (forrigeBehandling != null) beregningService.hentOptionalTilkjentYtelseForBehandling(behandlingId = forrigeBehandling.id)
                else null


        val krav: List<Krav> = BehandlingsresultatUtils.utledKrav(
                søknadDTO = søknadGrunnlagService.hentAktiv(behandlingId = behandlingId)?.hentSøknadDto(),
                forrigeAndelerTilkjentYtelse = forrigeTilkjentYtelse?.andelerTilkjentYtelse?.toList() ?: emptyList()
        )

        return BehandlingsresultatUtils.utledKravMedResultat(
                krav = krav.toList(),
                andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.toList(),
                forrigeAndelerTilkjentYtelse = forrigeTilkjentYtelse?.andelerTilkjentYtelse?.toList() ?: emptyList()
        )
    }
}




