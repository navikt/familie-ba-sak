package no.nav.familie.ba.sak.client

import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/norg2")
@ProtectedWithClaims(issuer = "azuread")
class Norg2Controller(
        private val norg2RestClient: Norg2RestClient
) {

    @GetMapping(path = ["/{enhet}"])
    fun hentEnhet(@PathVariable enhet: String): Enhet {
        return norg2RestClient.hentEnhet(enhet = enhet)
    }
}