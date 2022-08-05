package no.nav.familie.ba.sak.kjerne.verge

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VergeService(
    val behandlingService: BehandlingService
) {

    @Transactional
    fun RegistrerVergeForBehandling(behandling: Behandling, verge: Verge) {
        behandling.verge = verge
        behandlingService.lagre(behandling)
    }
}
