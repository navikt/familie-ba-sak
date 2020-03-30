package no.nav.familie.ba.sak.oppgave

import no.nav.familie.ba.sak.common.RessursResponse.badRequest
import no.nav.familie.integrasjoner.oppgave.domene.OppgaveDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/oppgave")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OppgaveController(val oppgaveService: OppgaveService) {
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun finnOppgaverKnyttetTilSaksbehandlerOgEnhet(@RequestParam("behandlingstema", required = false) behandlingstema: String?,
                                                   @RequestParam("oppgavetype", required = false) oppgavetype: String?,
                                                   @RequestParam("enhet", required = false) enhet: String?,
                                                   @RequestParam("saksbehandler", required = false) saksbehandler: String?)
            : ResponseEntity<Ressurs<List<OppgaveDto>?>> {

        if (!behandlingstema.isNullOrEmpty() && OppgaveService.Behandlingstema.values().all { it.kode != behandlingstema }) {
            return badRequest("Ugyldig behandlingstema", null)
        }

        return try {
            val oppgaver: List<OppgaveDto> = oppgaveService.finnOppgaverKnyttetTilSaksbehandlerOgEnhet(behandlingstema, oppgavetype, enhet, saksbehandler)
            ResponseEntity.ok().body(Ressurs.success<List<OppgaveDto>?>(oppgaver, "Finn oppgaver OK"))
        } catch (e: Throwable) {
            badRequest("Henting av oppgaver feilet", e)
        }
    }
}