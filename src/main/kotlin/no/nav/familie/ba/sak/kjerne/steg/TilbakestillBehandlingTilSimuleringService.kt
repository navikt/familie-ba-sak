package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilbakestillBehandlingTilSimuleringService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingService: BehandlingService,
) {
    @Transactional
    fun tilbakestillBehandlingTilSimuering(behandlingId: Long): Behandling {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        if (behandling.erTilbakestiltTilSimulering()) {
            return behandling
        }

        return behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
            behandlingId = behandlingId,
            steg = StegType.VURDER_TILBAKEKREVING,
        )
    }
}

private fun Behandling.erTilbakestiltTilSimulering(): Boolean {
    val gjeldendeSteg = this.behandlingStegTilstand.last()
    return gjeldendeSteg.behandlingSteg == StegType.VURDER_TILBAKEKREVING &&
        gjeldendeSteg.behandlingStegStatus == BehandlingStegStatus.IKKE_UTFØRT
}
