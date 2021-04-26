package no.nav.familie.ba.sak.infotrygd

import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.infotrygd.domene.MigreringResponseDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/migrering")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class MigreringController(private val migreringService: MigreringService) {

    @PostMapping
    fun migrer(@RequestBody personIdent: Personident,
               @RequestParam("behandlingAarsak") behandlingÅrsak: String): ResponseEntity<Ressurs<MigreringResponseDto>> {
        return ResponseEntity.ok(Ressurs.success(migreringService.migrer(personIdent.ident, BehandlingÅrsak.valueOf(behandlingÅrsak))))
    }
}