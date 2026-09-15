package no.nav.familie.ba.sak.internal.forvalterendepunkt

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.task.LogFagsakIdForJournalpostTask
import no.nav.familie.ba.sak.task.LogJournalpostIdForFagsakTask
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/forvalter/journalpost")
class ForvalterJournalpostController(
    private val tilgangService: TilgangService,
    private val taskRepository: TaskRepositoryWrapper,
) {
    @PostMapping("/hent-fagsak-id")
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
}
