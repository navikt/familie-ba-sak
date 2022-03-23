package no.nav.familie.ba.sak.kjerne.behandling.settpåvent

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Period

@Service
class SettPåVentService(
    val settPåVentRepository: SettPåVentRepository,
    val behandlingService: BehandlingService,
    val loggService: LoggService,
    val oppgaveService: OppgaveService,
    val toggleService: FeatureToggleService
) {
    fun finnAktivSettPåVentPåBehandling(behandlingId: Long): SettPåVent? {
        return settPåVentRepository.findByBehandlingIdAndAktiv(behandlingId, true)
    }

    fun finnAktiveSettPåVent(): List<SettPåVent> = settPåVentRepository.findByAktivTrue()

    fun finnAktivSettPåVentPåBehandlingThrows(behandlingId: Long): SettPåVent {
        return finnAktivSettPåVentPåBehandling(behandlingId)
            ?: throw Feil("Behandling $behandlingId er ikke satt på vent.")
    }

    @Transactional
    fun settBehandlingPåVent(behandlingId: Long, frist: LocalDate, årsak: SettPåVentÅrsak): SettPåVent {
        val behandling = behandlingService.hent(behandlingId)
        val gammelSettPåVent: SettPåVent? = finnAktivSettPåVentPåBehandling(behandlingId)
        validerBehandlingKanSettesPåVent(gammelSettPåVent, frist, behandling)

        loggService.opprettSettPåVentLogg(behandling, årsak.visningsnavn)
        logger.info("Sett på vent behandling $behandlingId med frist $frist og årsak $årsak")

        val settPåVent = settPåVentRepository.save(SettPåVent(behandling = behandling, frist = frist, årsak = årsak))
        behandlingService.sendTilDvh(behandling)

        oppgaveService.forlengOppgavefristerPåBehandling(
            behandlingId = behandling.id,
            forlengelse = Period.between(LocalDate.now(), frist)
        )

        return settPåVent
    }

    @Transactional
    fun oppdaterSettBehandlingPåVent(behandlingId: Long, frist: LocalDate, årsak: SettPåVentÅrsak): SettPåVent {
        val behandling = behandlingService.hent(behandlingId)
        val aktivSettPåVent = finnAktivSettPåVentPåBehandlingThrows(behandlingId)

        if (frist == aktivSettPåVent.frist && årsak == aktivSettPåVent.årsak) {
            throw FunksjonellFeil("Behandlingen er allerede satt på vent med frist $frist og årsak $årsak.")
        }

        loggService.opprettOppdaterVentingLogg(
            behandling = behandling,
            endretÅrsak = if (årsak != aktivSettPåVent.årsak) årsak.visningsnavn else null,
            endretFrist = if (frist != aktivSettPåVent.frist) frist else null,
        )
        logger.info("Oppdater sett på vent behandling $behandlingId med frist $frist og årsak $årsak")

        val gammelFrist = aktivSettPåVent.frist
        val settPåVent = settPåVentRepository.save(aktivSettPåVent.copy(frist = frist, årsak = årsak))

        behandlingService.sendTilDvh(behandlingService.hent(behandlingId))

        oppgaveService.forlengOppgavefristerPåBehandling(
            behandlingId = behandlingId,
            forlengelse = Period.between(gammelFrist, frist)
        )

        return settPåVentRepository.save(settPåVent)
    }

    fun gjenopptaBehandling(behandlingId: Long, nå: LocalDate = LocalDate.now()): SettPåVent {
        val behandling = behandlingService.hent(behandlingId)
        val aktivSettPåVent =
            finnAktivSettPåVentPåBehandling(behandlingId)
                ?: throw FunksjonellFeil(
                    melding = "Behandling $behandlingId er ikke satt på vent.",
                    frontendFeilmelding = "Behandlingen er ikke på vent og det er ikke mulig å gjenoppta behandling."
                )

        loggService.gjenopptaBehandlingLogg(behandling)
        logger.info("Gjenopptar behandling $behandlingId")

        aktivSettPåVent.aktiv = false
        aktivSettPåVent.tidTattAvVent = nå
        val settPåVent = settPåVentRepository.save(aktivSettPåVent)

        behandlingService.sendTilDvh(behandling)

        oppgaveService.settOppgavefristerPåBehandlingTil(
            behandlingId = behandlingId,
            nyFrist = LocalDate.now().plusDays(1)
        )

        return settPåVent
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(SettPåVentService::class.java)
        val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")
    }
}
