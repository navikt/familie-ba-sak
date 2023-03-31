package no.nav.familie.ba.sak.kjerne.beregning.endringstidspunkt

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
class EndringstidspunktController(
    val endringstidspunktService: EndringstidspunktService
) {
    @GetMapping("/behandlinger/{behanldingId}/endringstidspunkt")
    fun hentEndringstidspunkt(
        @PathVariable behanldingId: Long
    ): ResponseEntity<Ressurs<LocalDate>> = ResponseEntity.ok(
        Ressurs.success(
            endringstidspunktService.finnEndringstidspunktForBehandling(behanldingId)
        )
    )
}