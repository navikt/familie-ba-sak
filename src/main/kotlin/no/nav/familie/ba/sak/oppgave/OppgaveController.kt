package no.nav.familie.ba.sak.oppgave

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.toRestPersonInfo
import no.nav.familie.ba.sak.common.RessursUtils.badRequest
import no.nav.familie.ba.sak.common.RessursUtils.illegalState
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.oppgave.domene.DataForManuellJournalføring
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/oppgave")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OppgaveController(val oppgaveService: OppgaveService, val integrasjonClient: IntegrasjonClient) {

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun finnOppgaverKnyttetTilSaksbehandlerOgEnhet(@RequestParam("behandlingstema", required = false) behandlingstema: String?,
                                                   @RequestParam("oppgavetype", required = false) oppgavetype: String?,
                                                   @RequestParam("enhet", required = false) enhet: String?,
                                                   @RequestParam("saksbehandler", required = false) saksbehandler: String?)
            : ResponseEntity<Ressurs<List<Oppgave>>> {

        if (!behandlingstema.isNullOrEmpty() && OppgaveService.Behandlingstema.values().all { it.kode != behandlingstema }) {
            return badRequest("Ugyldig behandlingstema", null)
        }

        return try {
            val oppgaver: List<Oppgave> =
                    oppgaveService.finnOppgaverKnyttetTilSaksbehandlerOgEnhet(behandlingstema, oppgavetype, enhet, saksbehandler)
            ResponseEntity.ok().body(Ressurs.success(oppgaver, "Finn oppgaver OK"))
        } catch (e: Throwable) {
            badRequest("Henting av oppgaver feilet", e)
        }
    }

    @PostMapping(path = ["/{oppgaveId}/fordel"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fordelOppgave(@PathVariable(name = "oppgaveId") oppgaveId: Long,
                                      @RequestParam("saksbehandler") saksbehandler: String
    ): ResponseEntity<Ressurs<String>> {

        return Result.runCatching {
            oppgaveService.fordelOppgave(oppgaveId, saksbehandler)
        }.fold(
                onSuccess = { return ResponseEntity.ok().body(Ressurs.success(it)) },
                onFailure = { return illegalState("Feil ved tildeling av oppgave", it) }
        )
    }

    @PostMapping(path = ["/{oppgaveId}/tilbakestill"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun tilbakestillFordelingPåOppgave(@PathVariable(name = "oppgaveId") oppgaveId: Long): ResponseEntity<Ressurs<String>> {
        return Result.runCatching {
            oppgaveService.tilbakestillFordelingPåOppgave(oppgaveId)
        }.fold(
                onSuccess = { return ResponseEntity.ok().body(Ressurs.Companion.success(it)) },
                onFailure = { return illegalState("Feil ved tilbakestilling av tildeling på oppgave", it) }
        )
    }

    @GetMapping(path = ["/{oppgaveId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentDataForManuellJournalføring(@PathVariable(name = "oppgaveId") oppgaveId: Long)
            : ResponseEntity<Ressurs<DataForManuellJournalføring>> {

        return Result.runCatching {
            val oppgave = oppgaveService.hentOppgave(oppgaveId).data
                          ?: error("Feil ved henting av oppgave, data finnes ikke på ressurs")

            Ressurs.success(DataForManuellJournalføring(
                    oppgave = oppgave,
                    journalpost = if (oppgave.journalpostId == null) null else integrasjonClient.hentJournalpost(oppgave.journalpostId!!).data
                                                                               ?: error("Feil ved henting av journalpost, data finnes ikke på ressurs"),
                    person = if (oppgave.aktoerId == null) null else {
                        val personIdent = integrasjonClient.hentPersonIdent(oppgave.aktoerId)?.ident
                                          ?: error("Fant ikke personident for aktør id")

                        integrasjonClient.hentPersoninfoFor(personIdent).toRestPersonInfo(personIdent)
                    }
            ))
        }.fold(
                onSuccess = { return ResponseEntity.ok().body(it) },
                onFailure = { illegalState("Ukjent feil ved henting data for manuell journalføring.", it) }
        )
    }
}