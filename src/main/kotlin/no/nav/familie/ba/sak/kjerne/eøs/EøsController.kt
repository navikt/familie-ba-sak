package no.nav.familie.ba.sak.kjerne.eøs

import no.nav.familie.ba.sak.common.NullableMånedPeriode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/eøs")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class EøsController(val eøsService: EøsService) {
    @GetMapping(path = ["{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun utledEøsPerioder(
        @PathVariable behandlingId: Long
    ): ResponseEntity<Ressurs<Map<Person, List<NullableMånedPeriode>>>> {
        return ResponseEntity.ok(Ressurs.success(eøsService.utledEøsPerioder(behandlingId)))
    }
}
