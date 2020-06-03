package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.restDomene.RestPersonResultat
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/vilkaarsvurdering")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VilkårController(
        private val vilkårService: VilkårService
) {

    @PutMapping(path = ["/{behandlingId}/{vilkaarId}"])
    fun endreVilkår(@PathVariable behandlingId: Long,
                    @PathVariable vilkaarId: Long,
                    @RequestBody restPersonResultat: RestPersonResultat): ResponseEntity<Ressurs<List<RestPersonResultat>>> {
        val nyVilkårsvurdering = vilkårService.endreVilkår(behandlingId = behandlingId,
                                                           vilkårId = vilkaarId,
                                                           restPersonResultat = restPersonResultat)

        return ResponseEntity.ok(Ressurs.success(nyVilkårsvurdering))
    }
}

