package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.vedtak.Beslutning
import org.springframework.stereotype.Service

@Service
class ToTrinnKontrollService(private val behandlingService: BehandlingService) {

    fun valider2trinnVedBeslutningOmIverksetting(behandling: Behandling, beslutter: String, beslutning: Beslutning) {
        if (behandling.endretAv == beslutter) {
            error("Samme saksbehandler kan ikke foreslå og beslutte om iverksetting på samme vedtak")
        }
        behandlingService.oppdaterStatusPåBehandling(
                behandlingId = behandling.id,
                status = if (beslutning.erGodkjent()) BehandlingStatus.GODKJENT else BehandlingStatus.UNDERKJENT_AV_BESLUTTER)
    }
}
