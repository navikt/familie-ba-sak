package no.nav.familie.ba.sak.kjerne.behandling.settpåvent

import no.nav.familie.ba.sak.common.Feil
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

    fun finnAktivSettPåVentPåBehandlingThrows(behandlingId: Long): SettPåVent {
        return finnAktivSettPåVentPåBehandling(behandlingId)
            ?: throw Feil("Behandling $behandlingId er ikke satt på vent.",)
    }

    fun settBehandlingPåVent(behandlingId: Long, frist: LocalDate, årsak: SettPåVentÅrsak): SettPåVent {
        val behandling = behandlingService.hent(behandlingId)
        val settPåVent: SettPåVent? = finnAktivSettPåVentPåBehandling(behandlingId)
        validerBehandlingKanSettesPåVent(settPåVent, frist, behandling)

        return settPåVentRepository.save(SettPåVent(behandling = behandling, frist = frist, årsak = årsak))
    }

    fun oppdaterSettBehandlingPåVent(behandlingId: Long, frist: LocalDate, årsak: SettPåVentÅrsak): SettPåVent {
        val aktivSettPåVent = finnAktivSettPåVentPåBehandlingThrows(behandlingId)

        aktivSettPåVent.frist = frist
        aktivSettPåVent.årsak = årsak

        return settPåVentRepository.save(aktivSettPåVent)
    }

    fun deaktiverSettBehandlingPåVent(behandlingId: Long, nå: LocalDate = LocalDate.now()): SettPåVent {
        val aktivSettPåVent = finnAktivSettPåVentPåBehandlingThrows(behandlingId)

        aktivSettPåVent.aktiv = false
        aktivSettPåVent.frist = nå

        return settPåVentRepository.save(aktivSettPåVent)
    }
}
