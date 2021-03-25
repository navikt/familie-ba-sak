package no.nav.familie.ba.sak.infotrygd

import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/infotrygd")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class MigreringController(private val migreringService: MigreringService) {

    @PostMapping(path = ["/migrer"])
    fun migrer(@RequestBody personIdent: Personident,
               @RequestParam("behandlingAarsak") behandlingÅrsak: String): ResponseEntity<Ressurs<String>> {
        migreringService.migrer(personIdent.ident, BehandlingÅrsak.valueOf(behandlingÅrsak))
        return ResponseEntity.ok(Ressurs.success("Migrering påbegynt"))
    }
}