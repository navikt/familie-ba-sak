package no.nav.familie.ba.sak.kjerne.behandling.settpåvent

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SettPåVentService(
    val settPåVentRepository: SettPåVentRepository,
    val behandlingService: BehandlingService,
) {
    fun finnAktivSettPåVentPåBehandling(behandlingId: Long): SettPåVent? {
        return settPåVentRepository.findByBehandlingIdAndAktiv(behandlingId, true)
    }

    fun settbehandlingPåVent(behandlingId: Long, frist: LocalDate, årsak: SettPåVentÅrsak): SettPåVent {
        if (finnAktivSettPåVentPåBehandling(behandlingId) != null) {
            throw FunksjonellFeil(
                melding = "Behandling $behandlingId er allerede satt på vent.",
                frontendFeilmelding = "Behandling er allerede satt på vent.",
            )
        }

        val behandling = behandlingService.hent(behandlingId)

        return settPåVentRepository.save(SettPåVent(behandling = behandling, frist = frist, årsak = årsak))
    }

    fun oppdaterSettbehandlingPåVent(behandlingId: Long, frist: LocalDate, årsak: SettPåVentÅrsak): SettPåVent {
        val aktivSettPåVent = finnAktivSettPåVentPåBehandling(behandlingId)
            ?: throw FunksjonellFeil(
                melding = "Behandling $behandlingId er ikke satt på vent.",
                frontendFeilmelding = "Behandling er ikke satt på vent.",
            )

        return settPåVentRepository.save(aktivSettPåVent.copy(frist = frist, årsak = årsak))
    }

    fun deaktiverSettBehandlingPåVent(behandlingId: Long): SettPåVent {
        val aktivSettPåVent = finnAktivSettPåVentPåBehandling(behandlingId)
            ?: throw FunksjonellFeil(
                melding = "Behandling $behandlingId er ikke satt på vent.",
                frontendFeilmelding = "Behandling er ikke satt på vent.",
            )

        return settPåVentRepository.save(aktivSettPåVent.copy(aktiv = false))
    }
}
