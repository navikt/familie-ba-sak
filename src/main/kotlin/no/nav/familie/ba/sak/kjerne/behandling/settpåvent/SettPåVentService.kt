package no.nav.familie.ba.sak.kjerne.behandling.settpåvent

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SettPåVentService(
    val settPåVentRepository: SettPåVentRepository,
    val behandlingService: BehandlingService,
    val loggService: LoggService,
) {
    fun finnAktivSettPåVentPåBehandling(behandlingId: Long): SettPåVent? {
        return settPåVentRepository.findByBehandlingIdAndAktiv(behandlingId, true)
    }

    fun finnAktivSettPåVentPåBehandlingThrows(behandlingId: Long): SettPåVent {
        return finnAktivSettPåVentPåBehandling(behandlingId)
            ?: throw Feil("Behandling $behandlingId er ikke satt på vent.")
    }

    fun settBehandlingPåVent(behandlingId: Long, frist: LocalDate, årsak: SettPåVentÅrsak): SettPåVent {
        val behandling = behandlingService.hent(behandlingId)
        val settPåVent: SettPåVent? = finnAktivSettPåVentPåBehandling(behandlingId)
        validerBehandlingKanSettesPåVent(settPåVent, frist, behandling)

        loggService.opprettSettPåVentLogg(behandling, årsak.visningsnavn)

        return settPåVentRepository.save(SettPåVent(behandling = behandling, frist = frist, årsak = årsak))
    }

    fun oppdaterSettBehandlingPåVent(behandlingId: Long, frist: LocalDate, årsak: SettPåVentÅrsak): SettPåVent {
        val aktivSettPåVent = finnAktivSettPåVentPåBehandlingThrows(behandlingId)

        aktivSettPåVent.frist = frist
        aktivSettPåVent.årsak = årsak

        return settPåVentRepository.save(aktivSettPåVent)
    }

    fun gjenopptaBehandling(behandlingId: Long, nå: LocalDate = LocalDate.now()): SettPåVent {
        val behandling = behandlingService.hent(behandlingId)
        val aktivSettPåVent = finnAktivSettPåVentPåBehandlingThrows(behandlingId)

        aktivSettPåVent.aktiv = false
        aktivSettPåVent.tidTattAvVent = nå

        loggService.gjenopptaBehandlingLogg(behandling)

        return settPåVentRepository.save(aktivSettPåVent)
    }
}
