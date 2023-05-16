package no.nav.familie.ba.sak.kjerne.behandling.settpåvent

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Period

@Service
class SettPåVentService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
    private val settPåVentRepository: SettPåVentRepository,
    private val loggService: LoggService,
    private val oppgaveService: OppgaveService,
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
    fun lagreEllerOppdater(settPåVent: SettPåVent): SettPåVent {
        saksstatistikkEventPublisher.publiserBehandlingsstatistikk(behandlingId = settPåVent.behandling.id)
        return settPåVentRepository.save(settPåVent)
    }

    @Transactional
    fun settBehandlingPåVent(behandlingId: Long, frist: LocalDate, årsak: SettPåVentÅrsak): SettPåVent {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val gammelSettPåVent: SettPåVent? = finnAktivSettPåVentPåBehandling(behandlingId)
        validerBehandlingKanSettesPåVent(gammelSettPåVent, frist, behandling)

        loggService.opprettSettPåVentLogg(behandling, årsak.visningsnavn)
        logger.info("Sett på vent behandling $behandlingId med frist $frist og årsak $årsak")

        val settPåVent = lagreEllerOppdater(SettPåVent(behandling = behandling, frist = frist, årsak = årsak))

        oppgaveService.forlengFristÅpneOppgaverPåBehandling(
            behandlingId = behandling.id,
            forlengelse = Period.between(LocalDate.now(), frist),
        )

        return settPåVent
    }

    @Transactional
    fun oppdaterSettBehandlingPåVent(behandlingId: Long, frist: LocalDate, årsak: SettPåVentÅrsak): SettPåVent {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
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
        aktivSettPåVent.frist = frist
        aktivSettPåVent.årsak = årsak
        val settPåVent = lagreEllerOppdater(aktivSettPåVent)

        oppgaveService.forlengFristÅpneOppgaverPåBehandling(
            behandlingId = behandlingId,
            forlengelse = Period.between(gammelFrist, frist),
        )

        return settPåVent
    }

    fun gjenopptaBehandling(behandlingId: Long, nå: LocalDate = LocalDate.now()): SettPåVent {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val aktivSettPåVent =
            finnAktivSettPåVentPåBehandling(behandlingId)
                ?: throw FunksjonellFeil(
                    melding = "Behandling $behandlingId er ikke satt på vent.",
                    frontendFeilmelding = "Behandlingen er ikke på vent og det er ikke mulig å gjenoppta behandling.",
                )

        loggService.gjenopptaBehandlingLogg(behandling)
        logger.info("Gjenopptar behandling $behandlingId")

        aktivSettPåVent.aktiv = false
        aktivSettPåVent.tidTattAvVent = nå
        val settPåVent = lagreEllerOppdater(aktivSettPåVent)

        // Hvis behandlingen har vært på vent maskinellt, så må vi fjerne ev tilkjent ytelse og annet

        oppgaveService.settFristÅpneOppgaverPåBehandlingTil(
            behandlingId = behandlingId,
            nyFrist = LocalDate.now().plusDays(1),
        )

        return settPåVent
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(SettPåVentService::class.java)
        val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")
    }
}
