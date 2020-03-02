package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import org.springframework.stereotype.Service

@Service
class ToTrinnKontrollService(private val behandlingService: BehandlingService) {

    fun valider2trinnVedIverksetting(behandling: Behandling, ansvarligSaksbehandler: String) {
        if (behandling.endretAv == ansvarligSaksbehandler) {
            throw IllegalStateException("Samme saksbehandler kan ikke foreslå og iverksette samme vedtak")
        }
        behandlingService.oppdaterStatusPåBehandling(behandlingId = behandling.id, status = BehandlingStatus.GODKJENT)
    }
}
