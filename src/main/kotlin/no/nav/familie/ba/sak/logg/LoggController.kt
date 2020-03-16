package no.nav.familie.ba.sak.logg

import no.nav.familie.ba.sak.common.RessursResponse.badRequest
import no.nav.familie.ba.sak.validering.BehandlingstilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/logg")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class LoggController(
        private val loggService: LoggService
) {

    @GetMapping(path = ["/{behandlingId}"])
    fun hentLoggForBehandling(@PathVariable @BehandlingstilgangConstraint
                              behandlingId: Long): ResponseEntity<Ressurs<List<Logg>>> {
        return Result.runCatching { loggService.hentLoggForBehandling(behandlingId) }
                .fold(
                        onSuccess = { ResponseEntity.ok(Ressurs.success(it)) },
                        onFailure = {
                            badRequest("Henting av logg feilet", null) }
                )
    }
}