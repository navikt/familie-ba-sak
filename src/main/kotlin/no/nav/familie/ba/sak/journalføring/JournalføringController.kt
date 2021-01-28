package no.nav.familie.ba.sak.journalføring

import no.nav.familie.ba.sak.behandling.restDomene.RestJournalføring
import no.nav.familie.ba.sak.behandling.restDomene.RestOppdaterJournalpost
import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("/api/journalpost")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class JournalføringController(private val journalføringService: JournalføringService,
                              private val tilgangService: TilgangService) {

    @GetMapping(path = ["/{journalpostId}/hent"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentJournalpost(@PathVariable journalpostId: String)
            : ResponseEntity<Ressurs<Journalpost>> {
        return ResponseEntity.ok(journalføringService.hentJournalpost(journalpostId))
    }

    @GetMapping("/{journalpostId}/hent/{dokumentInfoId}")
    fun hentDokument(@PathVariable journalpostId: String,
                     @PathVariable dokumentInfoId: String)
            : ResponseEntity<Ressurs<ByteArray>> {
        return ResponseEntity.ok(Ressurs.success(journalføringService.hentDokument(journalpostId, dokumentInfoId), "OK"))
    }

    @Deprecated("bruk v2")
    @PutMapping(path = ["/{journalpostId}/ferdigstill/{oppgaveId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun journalfør(@PathVariable journalpostId: String,
                   @PathVariable oppgaveId: String,
                   @RequestParam(name = "journalfoerendeEnhet") journalførendeEnhet: String,
                   @RequestBody @Valid request: RestOppdaterJournalpost)
            : ResponseEntity<Ressurs<String>> {

        val fagsakId = journalføringService.ferdigstill(request, journalpostId, journalførendeEnhet, oppgaveId)
        return ResponseEntity.ok(Ressurs.success(fagsakId, "Journalpost $journalpostId Ferdigstilt"))
    }

    @PostMapping(path = ["/{journalpostId}/journalfør/{oppgaveId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun journalførV2(@PathVariable journalpostId: String,
                     @PathVariable oppgaveId: String,
                     @RequestParam(name = "journalfoerendeEnhet") journalførendeEnhet: String,
                     @RequestBody @Valid request: RestJournalføring)
            : ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER, handling = "journalføre")

        val fagsakId = journalføringService.journalfør(request, journalpostId, journalførendeEnhet, oppgaveId)
        return ResponseEntity.ok(Ressurs.success(fagsakId, "Journalpost $journalpostId Journalført"))
    }
}
