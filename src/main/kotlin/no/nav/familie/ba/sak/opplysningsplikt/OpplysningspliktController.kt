package no.nav.familie.ba.sak.opplysningsplikt

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/opplysningsplikt")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OpplysningspliktController(private val opplysningspliktService: OpplysningspliktService) {

    @GetMapping(path = ["/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentOpplysningsplikt(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<Opplysningsplikt?>> =
            ResponseEntity.ok(Ressurs.success(opplysningspliktService.hentOpplysningsplikt(behandlingId)))
}
