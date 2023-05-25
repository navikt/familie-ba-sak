package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ReaktiverBehandlingPåVentService(
    private val behandlingRepository: BehandlingRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * @param åpenBehandlingId brukes for validering
     * @param behandlingSomSniketIKøenId brukes for validering
     */
    fun reaktiverBehandlingPåVent(fagsakId: Long, åpenBehandlingId: Long, behandlingSomSniketIKøenId: Long) {
        val behandlingerForFagsak =
            behandlingRepository.finnBehandlinger(fagsakId).sortedByDescending { it.opprettetTidspunkt }
        if (behandlingerForFagsak.size < 2) {
            error("Forventet å finne fler enn 2 behandlinger på fagsak=$fagsakId")
        }
        val sisteBehandling = behandlingerForFagsak[0]
        val åpenBehandling = behandlingerForFagsak[1]

        validerBehandlinger(sisteBehandling, åpenBehandling, åpenBehandlingId, behandlingSomSniketIKøenId)

        aktiverBehandlingPåVent(sisteBehandling, åpenBehandling)
    }

    private fun aktiverBehandlingPåVent(
        sisteBehandling: Behandling,
        åpenBehandling: Behandling,
    ) {
        logger.info("Deaktiverer sisteBehandling=${sisteBehandling.id} og aktiverer åpenBehandling=${åpenBehandling.id}")
        sisteBehandling.aktiv = false

        åpenBehandling.aktiv = true
        åpenBehandling.aktivertTidspunkt = LocalDateTime.now()
        åpenBehandling.status = BehandlingStatus.UTREDES

        // TODO tilbakestill vedtak/brev etc på åpen behandling ?
        behandlingRepository.save(sisteBehandling)
        behandlingRepository.save(åpenBehandling)
    }

    private fun validerBehandlinger(
        sisteBehandling: Behandling,
        åpenBehandling: Behandling,
        åpenBehandlingId: Long,
        behandlingSomSniketIKøenId: Long,
    ) {
        if (sisteBehandling.id != behandlingSomSniketIKøenId) {
            error("Siste behandling=${sisteBehandling.id} for fagsak er ikke behandlingen som sniket i køen($behandlingSomSniketIKøenId)")
        }
        if (åpenBehandling.id != åpenBehandlingId) {
            error("Nest siste behandling=${åpenBehandling.id} for fagsak er ikke behandlingen som er satt på vent($åpenBehandlingId)")
        }
        validerTilstandPåBehandlinger(åpenBehandling, sisteBehandling)
    }

    private fun validerTilstandPåBehandlinger(
        åpenBehandling: Behandling,
        behandlingSomSniketIKøen: Behandling,
    ) {
        if (åpenBehandling.aktiv || åpenBehandling.status != BehandlingStatus.SATT_PÅ_MASKINELL_VENT) {
            error("Åpen behandling har feil tilstand $åpenBehandling")
        }
        if (!behandlingSomSniketIKøen.aktiv) {
            error("Behandling som sniket i køen må være aktiv $behandlingSomSniketIKøen")
        }
        if (behandlingSomSniketIKøen.status != BehandlingStatus.AVSLUTTET) {
            throw BehandlingErIkkeAvsluttetException(behandlingSomSniketIKøen)
        }
    }
}

class BehandlingErIkkeAvsluttetException(val behandling: Behandling) :
    RuntimeException("Behandling=${behandling.id} har status=${behandling.status} og er ikke avsluttet")
