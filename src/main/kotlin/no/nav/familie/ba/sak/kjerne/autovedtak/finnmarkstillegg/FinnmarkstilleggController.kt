package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.felles.Fødselsnummer
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/finnmarkstillegg")
@ProtectedWithClaims(issuer = "azuread")
class FinnmarkstilleggController(
    private val tilgangService: TilgangService,
    private val opprettTaskService: OpprettTaskService,
    private val fagsakService: FagsakService,
    private val personidentService: PersonidentService,
) {
    @PostMapping("/vurder-finnmarkstillegg")
    fun vurderFinnmarkstillegg(
        @RequestBody personIdent: PersonIdent,
    ): ResponseEntity<Ressurs<String>> {
        // Valider personIdent
        Fødselsnummer(personIdent.ident)

        tilgangService.validerTilgangTilPersoner(
            personIdenter = listOf(personIdent.ident),
            event = AuditLoggerEvent.UPDATE,
        )

        val aktør = personidentService.hentAktør(personIdent.ident)
        fagsakService
            .finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(aktør)
            .filter { it.type in FAGSAKTYPER_DER_FINNMARKSTILLEG_KAN_AUTOVEDTAS }
            .forEach { fagsak ->
                opprettTaskService.opprettAutovedtakFinnmarkstilleggTask(fagsak.id)
            }

        secureLogger.info("Trigget vurdering av Finnmarkstillegg for personIdent: ${personIdent.ident}")

        return ResponseEntity.ok(
            Ressurs.success("Trigget vurdering av Finnmarkstillegg"),
        )
    }
}
