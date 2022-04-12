package no.nav.familie.ba.sak.kjerne.tidslinje.rest

import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.TidslinjeService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tidslinjer")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class TidslinjeController(
    private val tidslinjeService: TidslinjeService
) {

    @GetMapping("/{behandlingId}")
    @Unprotected
    fun hentTidslinjer(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<RestTidslinjer>> {
        return ResponseEntity.ok(
            success(
                tidslinjeService.hentTidslinjerThrows(behandlingId).tilRestTidslinjer()
            )
        )
    }
}
