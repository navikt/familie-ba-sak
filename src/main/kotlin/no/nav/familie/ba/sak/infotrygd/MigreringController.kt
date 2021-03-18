package no.nav.familie.ba.sak.infotrygd

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/infotrygd")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class MigreringController(private val migreringService: MigreringService) {

    @PostMapping(path = ["/migrer"])
    fun migrer(@RequestBody personIdent: Personident): ResponseEntity<Ressurs<String>> {
        migreringService.migrer(personIdent.ident)
        return ResponseEntity.ok(Ressurs.success("Migrering påbegynt"))
    }
}