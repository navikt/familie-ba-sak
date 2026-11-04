package no.nav.familie.ba.sak.internal.forvalterendepunkter

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.OppgaveRepository
import no.nav.familie.ba.sak.internal.tabulærtest.GenererTabulærTestDto
import no.nav.familie.ba.sak.internal.tabulærtest.GenererTabulærTestService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.ba.sak.task.LogFagsakIdForJournalpostTask
import no.nav.familie.ba.sak.task.LogJournalpostIdForFagsakTask
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/forvalter")
class ForvalterController(
    private val oppgaveRepository: OppgaveRepository,
    private val integrasjonKlient: IntegrasjonKlient,
    private val genererTabulærTestService: GenererTabulærTestService,
    private val tilgangService: TilgangService,
    private val taskRepository: TaskRepositoryWrapper,
    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
) {
    private val logger: Logger = LoggerFactory.getLogger(ForvalterController::class.java)

    @PostMapping(
        path = ["/ferdigstill-oppgaver"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun ferdigstillListeMedOppgaver(
        @RequestBody oppgaveListe: List<Long>,
    ): ResponseEntity<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Ferdigstill liste med oppgaver",
        )

        var antallFeil = 0
        oppgaveListe.forEach { oppgaveId ->
            Result
                .runCatching {
                    ferdigstillOppgave(oppgaveId)
                }.fold(
                    onSuccess = { logger.info("Har ferdigstilt oppgave med oppgaveId=$oppgaveId") },
                    onFailure = {
                        logger.warn("Klarte ikke å ferdigstille oppgaveId=$oppgaveId", it)
                        antallFeil = antallFeil.inc()
                    },
                )
        }
        return ResponseEntity.ok("Ferdigstill oppgaver kjørt. Antall som ikke ble ferdigstilt: $antallFeil")
    }

    private fun ferdigstillOppgave(oppgaveId: Long) {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Ferdigstill oppgave",
        )

        integrasjonKlient.ferdigstillOppgave(oppgaveId)
        oppgaveRepository.findByGsakId(oppgaveId.toString()).also {
            if (it != null && !it.erFerdigstilt) {
                it.erFerdigstilt = true
                oppgaveRepository.saveAndFlush(it)
            }
        }
    }

    @Operation(
        summary = "Generer testgrunnlag",
        description = "Fagsaker, behandlinger og persongrunnlag er alltid med. Øvrige databolker og forrige vedtatte behandling kan velges. EØS-data er ikke med som standard. Handling og forventninger må legges til manuelt.",
    )
    @PostMapping(path = ["/generer-test"], consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun genererTest(
        @RequestBody genererTabulærTestDto: GenererTabulærTestDto,
    ): String {
        tilgangService.validerTilgangTilBehandling(behandlingId = genererTabulærTestDto.behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Generere test",
        )

        return genererTabulærTestService
            .genererTest(genererTabulærTestDto)
            .replace("\n", System.lineSeparator())
    }

    @PostMapping("/hent-fagsak-id-for-journalpost")
    @Operation(
        summary = "Henter fagsak id som er koblet til journalposten",
        description = "Oppretter task for å logge fagsak id som er koblet til journalpost. Fagsak id'n logges til securelog.",
    )
    fun hentFagsakIdForJournalpost(
        @RequestParam("journalpostId") journalpostId: String,
    ): ResponseEntity<Long> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Hente fagsakId for journalpost",
        )

        val opprettetTask = taskRepository.save(LogFagsakIdForJournalpostTask.opprettTask(journalpostId))

        return ResponseEntity.ok(opprettetTask.id)
    }

    @PostMapping("/hent-journalpost-id-for-fagsak")
    @Operation(
        summary = "Henter journalpost ider koblet til fagsaken",
        description = "Oppretter task for å logge journalpost id som er koblet til en fagsak. Journalpost ider logges til securelog.",
    )
    fun hentJournalpostIdForFagsak(
        @RequestParam("fagsakId") fagsakId: String,
    ): ResponseEntity<Long> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Hente journalpostId for fagsak",
        )

        val opprettetTask = taskRepository.save(LogJournalpostIdForFagsakTask.opprettTask(fagsakId))

        return ResponseEntity.ok(opprettetTask.id)
    }

    @PostMapping("/send-behandlingsstatistikk-til-dvh")
    fun sendBehandlingsstatistikkTilDvh(
        @RequestBody behandlingId: Long,
    ): ResponseEntity<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Sende behandlingsstatistikk for behandling til Datavarehus",
        )
        saksstatistikkEventPublisher.publiserBehandlingsstatistikk(behandlingId)
        return ResponseEntity.ok("Sendt behandlingsstatistikk for behandling $behandlingId til Datavarehus")
    }
}
