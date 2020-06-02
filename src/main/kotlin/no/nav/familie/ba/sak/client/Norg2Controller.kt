package no.nav.familie.ba.sak.client

import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/norg2")
@Unprotected
class Norg2Controller(
        private val norg2RestClient: Norg2RestClient
) {

    @GetMapping(path = ["/{enhet}"])
    fun hentEnhet(@PathVariable enhet: String): Any {
        return norg2RestClient.hentEnhet(enhet = enhet)
    }
}