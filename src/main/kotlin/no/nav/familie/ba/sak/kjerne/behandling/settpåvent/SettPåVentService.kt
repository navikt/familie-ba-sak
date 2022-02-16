package no.nav.familie.ba.sak.kjerne.behandling.settpåvent

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
        val gammelSettPåVent: SettPåVent? = finnAktivSettPåVentPåBehandling(behandlingId)
        validerBehandlingKanSettesPåVent(gammelSettPåVent, frist, behandling)

        loggService.opprettSettPåVentLogg(behandling, årsak.visningsnavn)
        logger.info("Sett på vent behandling $behandlingId med frist $frist og årsak $årsak")

        val settPåVent = settPåVentRepository.save(SettPåVent(behandling = behandling, frist = frist, årsak = årsak))
        behandlingService.sendTilDvh(behandling)

        return settPåVent
    }

    fun oppdaterSettBehandlingPåVent(behandlingId: Long, frist: LocalDate, årsak: SettPåVentÅrsak): SettPåVent {
        val aktivSettPåVent = finnAktivSettPåVentPåBehandlingThrows(behandlingId)

        logger.info("Oppdater sett på vent behandling $behandlingId med frist $frist og årsak $årsak")

        aktivSettPåVent.frist = frist
        aktivSettPåVent.årsak = årsak
        val settPåVent = settPåVentRepository.save(aktivSettPåVent)

        behandlingService.sendTilDvh(behandlingService.hent(behandlingId))

        return settPåVent
    }

    fun gjenopptaBehandling(behandlingId: Long, nå: LocalDate = LocalDate.now()): SettPåVent {
        val behandling = behandlingService.hent(behandlingId)
        val aktivSettPåVent = finnAktivSettPåVentPåBehandlingThrows(behandlingId)

        loggService.gjenopptaBehandlingLogg(behandling)
        logger.info("Gjenopptar behandling $behandlingId")

        aktivSettPåVent.aktiv = false
        aktivSettPåVent.tidTattAvVent = nå
        val settPåVent = settPåVentRepository.save(aktivSettPåVent)

        behandlingService.sendTilDvh(behandling)

        return settPåVent
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(SettPåVentService::class.java)
        val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")
    }
}
