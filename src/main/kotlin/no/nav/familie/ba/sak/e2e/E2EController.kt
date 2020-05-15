package no.nav.familie.ba.sak.e2e

import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/e2e")
@ProtectedWithClaims(issuer = "azuread")
@Profile("e2e")
class E2EController(
        private val databaseCleanupService: DatabaseCleanupService
) {

    @GetMapping(path = ["/truncate"])
    fun truncate(): ResponseEntity<String> {
        databaseCleanupService.truncate()

        return ResponseEntity.ok("Truncate fullført")
    }
}