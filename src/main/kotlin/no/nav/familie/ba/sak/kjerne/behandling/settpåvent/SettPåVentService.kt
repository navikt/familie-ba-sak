package no.nav.familie.ba.sak.kjerne.behandling.settpåvent

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SettPåVentService(
    val settPåVentRepository: SettPåVentRepository
    val behandlingService: BehandlingService,
) {
    fun finnSettPåVentPåBehandling(behandlingId: Long): List<SettPåVent>? {
        return settPåVentRepository.findByBehandlingId(behandlingId)
    }

    fun finnAktivSettPåVentPåBehandling(behandlingId: Long): SettPåVent? {
        return settPåVentRepository.findByBehandlingIdAndAktiv(behandlingId, true)
    }

    fun settbehandlingPåVent(behandlingId: Long, frist: LocalDate, årsak: SettPåVentÅrsak): SettPåVent? {
        val behandling = behandlingService.
        val settPåVent = SettPåVent(behandlingId, frist, årsak)
        return settPåVentRepository.findByBehandlingIdAndAktiv(behandlingId, true)
    }
}
