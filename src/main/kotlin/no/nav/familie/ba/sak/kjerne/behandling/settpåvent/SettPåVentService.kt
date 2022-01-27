package no.nav.familie.ba.sak.kjerne.behandling.settpåvent

import no.nav.familie.ba.sak.common.Feil
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

    fun finnAktivSettPåVentPåBehandlingThrows(behandlingId: Long): SettPåVent {
        return finnAktivSettPåVentPåBehandling(behandlingId)
            ?: throw Feil("Behandling $behandlingId er ikke satt på vent.",)
    }

    fun settBehandlingPåVent(behandlingId: Long, frist: LocalDate, årsak: SettPåVentÅrsak): SettPåVent {
        if (finnAktivSettPåVentPåBehandling(behandlingId) != null) {
            throw Feil(
                "Behandling $behandlingId er allerede satt på vent."

            )
        }

        if (frist.isBefore(LocalDate.now())) {
            throw FunksjonellFeil(
                melding = "Frist for å vente på behandling $behandlingId er satt før dagens dato.",
                frontendFeilmelding = "Fristen er satt før dagens dato.",
            )
        }

        val behandling = behandlingService.hent(behandlingId)

        return settPåVentRepository.save(SettPåVent(behandling = behandling, frist = frist, årsak = årsak))
    }

    fun oppdaterSettBehandlingPåVent(behandlingId: Long, frist: LocalDate, årsak: SettPåVentÅrsak): SettPåVent {
        val aktivSettPåVent = finnAktivSettPåVentPåBehandlingThrows(behandlingId)

        return settPåVentRepository.save(aktivSettPåVent.copy(frist = frist, årsak = årsak))
    }

    fun deaktiverSettBehandlingPåVent(behandlingId: Long, nå: LocalDate = LocalDate.now()): SettPåVent {
        val aktivSettPåVent = finnAktivSettPåVentPåBehandlingThrows(behandlingId)

        return settPåVentRepository.save(aktivSettPåVent.copy(aktiv = false, tidTattAvVent = nå))
    }
}
