package no.nav.familie.ba.sak.journalføring

import no.nav.familie.ba.sak.journalføring.domene.Journalpost
import no.nav.familie.ba.sak.journalføring.domene.OppdaterJournalpostRequest
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@RequestMapping("/api/journalføring")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class JournalføringtController(val journalføringService: JournalføringService) {

    @GetMapping(path = ["/hent/{journalpostId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentJournalpost(@PathVariable journalpostId: String)
        : ResponseEntity<Ressurs<Journalpost>> {
        return ResponseEntity.ok(Ressurs.success(journalføringService.hentJournalpost(journalpostId), "OK"))
    }

    @GetMapping("/hent/{journalpostId}/{dokumentInfoId}")
    fun hentDokument(@PathVariable journalpostId: String,
                     @PathVariable dokumentInfoId: String)
        : ResponseEntity<Ressurs<ByteArray>> {
        return ResponseEntity.ok(Ressurs.success(journalføringService.hentDokument(journalpostId, dokumentInfoId), "OK"))
    }

    @PutMapping(path = ["/ferdigstill/{journalpostId}/{oppgaveId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun journalfør(@PathVariable journalpostId: String,
                   @PathVariable oppgaveId: String,
                   @RequestParam(name = "journalfoerendeEnhet") journalførendeEnhet: String,
                   @RequestBody @Valid oppdaterJournalpostRequest: OppdaterJournalpostRequest)
        : ResponseEntity<Ressurs<String>> {

        journalføringService.ferdigstill(oppdaterJournalpostRequest, journalpostId, journalførendeEnhet, oppgaveId)
        return ResponseEntity.ok(Ressurs.success("OK", "Journalpost $journalpostId Ferdigstilt"))
    }
}