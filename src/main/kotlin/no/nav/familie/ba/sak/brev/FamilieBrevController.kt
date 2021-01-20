package no.nav.familie.ba.sak.brev

import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/familiebrev")
//@ProtectedWithClaims(issuer = "azuread")
@Unprotected
@Validated
class FamilieBrevController(
        private val familieBrevService: FamilieBrevService
) {

    @PostMapping(path = ["/{målform}/{malnavn}"],
                 produces = [MediaType.APPLICATION_PDF_VALUE])
    fun genererVedtaksbrev(@PathVariable målform: String, @PathVariable malnavn: String): ByteArray {
        return familieBrevService.genererBrevTest(målform, malnavn)
    }
}