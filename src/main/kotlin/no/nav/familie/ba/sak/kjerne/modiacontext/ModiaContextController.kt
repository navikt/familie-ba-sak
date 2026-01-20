package no.nav.familie.ba.sak.kjerne.modiacontext

import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.ekstern.restDomene.NyAktivBrukerIModiaContextDto
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/modia-context")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class ModiaContextController(
    private val modiaContextService: ModiaContextService,
    private val tilgangService: TilgangService,
) {
    @GetMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentContext(): ResponseEntity<Ressurs<ModiaContext>> = ok(success(modiaContextService.hentContext()))

    @PostMapping(
        path = ["/sett-aktiv-bruker"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun settNyAktivBruker(
        @RequestBody nyAktivBruker: NyAktivBrukerIModiaContextDto,
    ): ResponseEntity<Ressurs<ModiaContext>> {
        tilgangService.validerTilgangTilPersoner(
            personIdenter = listOf(nyAktivBruker.personIdent),
            event = AuditLoggerEvent.ACCESS,
        )
        return ok(success(modiaContextService.settNyAktivBruker(nyAktivBruker)))
    }
}
