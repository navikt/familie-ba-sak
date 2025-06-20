package no.nav.familie.ba.sak.integrasjoner.ainntekt

import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Fødselsnummer
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/a-inntekt")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class AInntektController(
    private val tilgangService: TilgangService,
    private val aInntektService: AInntektService,
) {
    @PostMapping("hent-url")
    fun hentAInntektUrl(
        @RequestBody personIdent: PersonIdent,
    ): Ressurs<String> {
        // Valider personIdent
        Fødselsnummer(personIdent.ident)

        tilgangService.validerTilgangTilPersoner(
            personIdenter = listOf(personIdent.ident),
            event = AuditLoggerEvent.ACCESS,
        )

        return Ressurs.success(aInntektService.hentAInntektUrl(personIdent))
    }
}
