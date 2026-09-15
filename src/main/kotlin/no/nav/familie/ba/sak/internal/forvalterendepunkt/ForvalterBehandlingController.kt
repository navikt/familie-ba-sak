package no.nav.familie.ba.sak.internal.forvalterendepunkt

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.MaskineltUnderkjennVedtakTask
import no.nav.familie.ba.sak.task.dto.FerdigstillBehandlingDTO
import no.nav.familie.ba.sak.task.dto.HenleggAutovedtakOgSettBehandlingTilbakeTilVentVedSmåbarnstilleggTask
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/forvalter/behandling")
class ForvalterBehandlingController(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingService: BehandlingService,
    private val tilgangService: TilgangService,
    private val taskService: TaskService,
    private val taskRepository: TaskRepositoryWrapper,
) {
    private val logger: Logger = LoggerFactory.getLogger(ForvalterBehandlingController::class.java)

    @PutMapping("/{behandlingId}/maskinelt-underkjenn-vedtak")
    @Operation(summary = "Underkjenner et vedtak på vegne av system")
    fun maskineltUnderkjennVedtak(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Underkjenner et vedtak på vegne av system",
        )

        val task = taskService.save(MaskineltUnderkjennVedtakTask.opprettTask(behandlingId))
        return ResponseEntity.ok(Ressurs.success("Underkjenner vedtak i behandling $behandlingId i task ${task.id}"))
    }

    @PostMapping("/henlegg-autovedtak-og-sett-behandling-tilbake-paa-vent")
    @Operation(summary = "Henlegger autovedtak og setter behandling tilbake på vent.")
    @Transactional
    fun henleggAutovedtakOgSettBehandlingTilbakePåVent(
        @RequestBody behandlingList: List<Long>,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Henlegger autovedtak og setter behandling tilbake på vent.",
        )

        behandlingList.forEach { behandlingId ->
            logger.info("Opprettet oppdaterLøpendeFlaggTask for behandlingId=$behandlingId")
            val hennleggAutovedtakTask = HenleggAutovedtakOgSettBehandlingTilbakeTilVentVedSmåbarnstilleggTask.opprettTask(behandlingId)
            taskRepository.save(hennleggAutovedtakTask)
        }
        return ResponseEntity.ok(Ressurs.success("Task for henleggelse av autovedtak startet"))
    }

    @PatchMapping("/{behandlingId}/deaktiverHenlagtBehandlingOgSettSisteVedtatteBehandlingTilAktiv")
    @Operation(
        summary = "Deaktiverer en behandling og setter siste vedtatte behandling til aktiv",
        description = "Dette endepunktet deaktiverer en behandling og setter siste vedtatte behandling til aktiv. Dette for å fikse en sak som ble feil etter en bug i henlegging",
    )
    @Transactional
    fun deaktiverHenlagtBehandlingOgSettSisteVedtatteBehandlingTilAktiv(
        @PathVariable behandlingId: Long,
    ) {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Deaktiverer en behandling og setter siste vedtatte behandling til aktiv",
        )

        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val aktivBehandling = behandlingHentOgPersisterService.finnAktivForFagsak(behandling.fagsak.id) ?: error("Finnes ingen aktiv behandling")
        if (aktivBehandling.id != behandling.id) {
            error("Aktiv behandling er ${aktivBehandling.id} og ikke ${behandling.id}")
        }

        if (!aktivBehandling.erHenlagt()) {
            error("Aktiv behandling er ikke henlagt")
        }

        aktivBehandling.aktiv = false
        behandlingHentOgPersisterService.lagreEllerOppdater(aktivBehandling, sendTilDvh = false)
        behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)?.apply {
            aktiv = true
            behandlingHentOgPersisterService.lagreEllerOppdater(this, sendTilDvh = false)
            secureLogger.info("Patchet ${behandling.fagsak.id} ved å deaktivere behandling ${behandling.id} og setter siste vedtatte behandling ($id) til aktiv")
        }
    }

    @PostMapping("/opprett-ferdigstill-behandling-task")
    @Operation(
        summary = "Oppretter en ferdigstill behandling task for en behandling",
        description = """
            Kan brukes hvis distribuer dokument har blitt avvikshåndtert og behandlingen ikke har blitt ferdigstilt.

            Hvis man ikke fikk distribuert brevet gjennom dokdist og saksbehandler manuelt har sendt printet og sendt, så vil 
            aldri behandlingen gå viderere til ferdigstill behandling-steget. I de tilfellene kan man bruke dette endepunktet
        """,
    )
    fun opprettFerdigstillBehandlingTask(
        @RequestBody dto: FerdigstillBehandlingDTO,
    ): ResponseEntity<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Opprett task for å ferdigstille behandling for ident",
        )

        val behandling = behandlingHentOgPersisterService.hent(dto.behandlingsId)
        if (behandling.status != BehandlingStatus.IVERKSETTER_VEDTAK) {
            return ResponseEntity.badRequest().body("Kan bare opprette ferdigstill behandling task for behandlinger som er i status IVERKSETTER_VEDTAK")
        }

        if (behandling.steg != StegType.DISTRIBUER_VEDTAKSBREV) {
            return ResponseEntity.badRequest().body("Kan bare opprette ferdigstill behandling task for behandlinger som er i steg DISTRIBUER_VEDTAKSBREV")
        }

        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
            behandlingId = dto.behandlingsId,
            steg = StegType.FERDIGSTILLE_BEHANDLING,
        )

        val task =
            FerdigstillBehandlingTask.opprettTask(
                søkerIdent = dto.personIdent,
                behandlingsId = dto.behandlingsId,
            )
        taskRepository.save(task)

        logger.info("Opprettet ferdigstill behandling task for behandling ${dto.behandlingsId} gjennom forvalter-endepunktet")

        return ResponseEntity.ok("Task opprettet ${task.id}")
    }
}
