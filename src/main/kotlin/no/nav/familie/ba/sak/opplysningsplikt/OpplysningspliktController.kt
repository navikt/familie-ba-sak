package no.nav.familie.ba.sak.opplysningsplikt

import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.*
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/opplysningsplikt")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OpplysningspliktController(
        private val opplysningspliktService: OpplysningspliktService,
        private val fagsakService: FagsakService,
) {
    @PutMapping(path = ["/{fagsakId}/{behandlingId}"])
    fun oppdaterOpplysningsplikt(@PathVariable fagsakId: Long,
                                 @PathVariable behandlingId: Long,
                                 @RequestBody restOpplysningsplikt: RestOpplysningsplikt): ResponseEntity<Ressurs<RestFagsak>> {

        opplysningspliktService.oppdaterOpplysningsplikt(behandlingId, restOpplysningsplikt)
        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId))
    }
}