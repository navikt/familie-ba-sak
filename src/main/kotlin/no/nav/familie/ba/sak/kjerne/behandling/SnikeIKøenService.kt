package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SnikeIKøenService(
    private val behandlingRepository: BehandlingRepository,
    private val påVentService: SettPåVentService,
    private val loggService: LoggService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun settAktivBehandlingTilPåMaskinellVent(behandlingId: Long, årsak: SettPåMaskinellVentÅrsak) {
        val behandling = behandlingRepository.finnBehandling(behandlingId)
        if (!behandling.aktiv) {
            error("Behandling=$behandlingId er ikke aktiv")
        }
        val behandlingStatus = behandling.status
        if (behandlingStatus !== BehandlingStatus.UTREDES && behandlingStatus !== BehandlingStatus.SATT_PÅ_VENT) {
            error("Behandling=$behandlingId kan ikke settes på maskinell vent då status=$behandlingStatus")
        }
        behandling.status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT
        behandling.aktiv = false
        behandlingRepository.saveAndFlush(behandling)
        loggService.opprettSettPåMaskinellVentSatsendring(behandling, årsak.årsak)
    }

    /**
     * @param behandlingPåVentId brukes for validering
     * @param behandlingSomSnekIKøenId brukes for validering
     */
    @Transactional
    fun reaktiverBehandlingPåMaskinellVent(fagsakId: Long, behandlingPåVentId: Long, behandlingSomSnekIKøenId: Long) {
        val behandlingerForFagsak =
            behandlingRepository.finnBehandlinger(fagsakId).sortedByDescending { it.opprettetTidspunkt }
        if (behandlingerForFagsak.size < 2) {
            error("Forventet å finne fler enn 2 behandlinger på fagsak=$fagsakId")
        }
        val behandlingSomSnekIKøen = behandlingerForFagsak[0]
        val behandlingPåVent = behandlingerForFagsak[1]

        validerBehandlinger(behandlingPåVent, behandlingPåVentId, behandlingSomSnekIKøen, behandlingSomSnekIKøenId)

        aktiverBehandlingPåVent(behandlingSomSnekIKøen, behandlingPåVent)
    }

    private fun aktiverBehandlingPåVent(
        behandlingSomSnekIKøen: Behandling,
        behandlingPåVent: Behandling,
    ) {
        logger.info("Deaktiverer behandlingSomSnekIKøen=${behandlingSomSnekIKøen.id} og aktiverer behandlingPåVent=${behandlingPåVent.id}")
        behandlingSomSnekIKøen.aktiv = false

        behandlingRepository.saveAndFlush(behandlingSomSnekIKøen)
        behandlingPåVent.aktiv = true
        behandlingPåVent.aktivertTidspunkt = LocalDateTime.now()
        behandlingPåVent.status = utledStatusForBehandlingPåVent(behandlingPåVent)

        // TODO burde det legges inn hendelse i loggService om at den er tatt av vent?
        // TODO tilbakestill vedtak/brev etc på åpen behandling ?
        behandlingRepository.saveAndFlush(behandlingPåVent)
    }

    /**
     * Hvis behandlingen er satt på vent av saksbehandler så skal statusen settes tilbake til SATT_PÅ_VENT
     * Ellers settes UTREDES
     */
    private fun utledStatusForBehandlingPåVent(behandlingPåVent: Behandling) =
        påVentService.finnAktivSettPåVentPåBehandling(behandlingPåVent.id)
            ?.let { BehandlingStatus.SATT_PÅ_VENT }
            ?: BehandlingStatus.UTREDES

    private fun validerBehandlinger(
        nestSisteBehandling: Behandling,
        behandlingPåVentId: Long,
        sisteBehandling: Behandling,
        behandlingSomSnekIKøenId: Long,
    ) {
        if (sisteBehandling.id != behandlingSomSnekIKøenId) {
            error("Siste behandling=${sisteBehandling.id} for fagsak er ikke behandlingen som snek i køen($behandlingSomSnekIKøenId)")
        }
        if (nestSisteBehandling.id != behandlingPåVentId) {
            error("Nest siste behandling=${nestSisteBehandling.id} for fagsak er ikke behandlingen som er satt på vent($behandlingPåVentId)")
        }
        validerTilstandPåBehandlinger(nestSisteBehandling, sisteBehandling)
    }

    private fun validerTilstandPåBehandlinger(
        behandlingPåVent: Behandling,
        behandlingSomSnekIKøen: Behandling,
    ) {
        if (behandlingPåVent.aktiv || behandlingPåVent.status != BehandlingStatus.SATT_PÅ_MASKINELL_VENT) {
            error("Åpen behandling har feil tilstand $behandlingPåVent")
        }
        if (!behandlingSomSnekIKøen.aktiv) {
            error("Behandling som snek i køen må være aktiv $behandlingSomSnekIKøen")
        }
        if (behandlingSomSnekIKøen.status != BehandlingStatus.AVSLUTTET) {
            throw BehandlingErIkkeAvsluttetException(behandlingSomSnekIKøen)
        }
    }
}

enum class SettPåMaskinellVentÅrsak(val årsak: String) {
    SATSENDRING("Satsendring"),
}

class BehandlingErIkkeAvsluttetException(val behandling: Behandling) :
    RuntimeException("Behandling=${behandling.id} har status=${behandling.status} og er ikke avsluttet")
