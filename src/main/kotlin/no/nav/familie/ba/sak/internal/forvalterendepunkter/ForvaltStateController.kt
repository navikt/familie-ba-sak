package no.nav.familie.ba.sak.internal.forvalterendepunkter

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.internal.ForvalterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.OppdaterLøpendeFlagg
import no.nav.familie.ba.sak.task.SlettKompetanserTask
import no.nav.familie.ba.sak.task.dto.FerdigstillBehandlingDTO
import no.nav.familie.ba.sak.task.dto.HenleggAutovedtakOgSettBehandlingTilbakeTilVentVedSmåbarnstilleggTask
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/forvalter")
class ForvaltStateController(
    private val tilgangService: TilgangService,
    private val forvalterService: ForvalterService,
    private val taskService: TaskService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingService: BehandlingService,
    private val taskRepository: TaskRepositoryWrapper,
) {
    private val logger: Logger = LoggerFactory.getLogger(ForvaltStateController::class.java)

    @DeleteMapping("/slett-alle-kompetanser-for-behandling/{behandlingId}")
    @Operation(summary = "Slett kompetanser, utenlandsk periodebeløp og valutakurser for en behandling som er på/står fast på vilkårsvurderingssteget.")
    fun slettKompetanser(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Slett kompetanser, utenlandsk periodebeløp og valutakurser for en behandling som er på vilkårsvurderingssteget.",
        )

        val task = taskService.save(SlettKompetanserTask.opprettTask(behandlingId))
        return ResponseEntity.ok(Ressurs.success("Kompetanser slettes i task ${task.id}"))
    }

    @PostMapping("/kjør-oppdater-løpende-flagg-task")
    @Operation(summary = "Kjører oppdaterLøpendeFlagg-tasken slik at man oppdaterer tasker som er løpende til avsluttet ved behov.")
    fun kjørOppdaterLøpendeFlaggTask(): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Kjører oppdaterLøpendeFlagg-tasken slik at man oppdaterer tasker som er løpende til avsluttet ved behov.",
        )

        val oppdaterLøpendeFlaggTask = Task(type = OppdaterLøpendeFlagg.TASK_STEP_TYPE, payload = "")
        taskRepository.save(oppdaterLøpendeFlaggTask)
        logger.info("Opprettet oppdaterLøpendeFlaggTask")
        return ResponseEntity.ok(Ressurs.success("Task for oppdatering av løpende flagg startet"))
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

    @PatchMapping("/fagsak/{fagsakId}/endre-status-til-opprettet")
    @Operation(
        summary = " Endrer fagsakstatus fra løpende til opprettet om det kun finnes henlagte behandlinger.",
        description = "En fagsak som har status løpende uten vedtatte behandlinger feiler opprettelse av revurdering. I en fødselshendelse med tvillinger hvor første task gir henlagt behandling, og andre task skal vedta behandling, må man endre status på fagsak til opprettet for å kunne kjøre automatisk behandling.",
    )
    fun endreStatusPåFagsak(
        @PathVariable fagsakId: Long,
    ): ResponseEntity<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Endre status på fagsak",
        )

        forvalterService.endreFagsakStatusFraLøpendeTilOpprettet(fagsakId)
        return ResponseEntity.ok("Endret status på fagsak $fagsakId fra løpende til opprettet.")
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
