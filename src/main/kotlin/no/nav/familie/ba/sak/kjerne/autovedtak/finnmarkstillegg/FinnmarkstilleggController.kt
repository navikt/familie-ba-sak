package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.sikkerhet.TilgangService
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

        // TODO: Implementer mot logikken for å trigge vurdering av Finnmarkstillegg
        secureLogger.info("Trigget vurdering av Finnmarkstillegg for personIdent: ${personIdent.ident}")

        return ResponseEntity.ok(
            Ressurs.success("Trigget vurdering av Finnmarkstillegg"),
        )
    }
}
