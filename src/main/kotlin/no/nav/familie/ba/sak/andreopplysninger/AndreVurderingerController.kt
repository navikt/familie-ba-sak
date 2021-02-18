package no.nav.familie.ba.sak.andreopplysninger

import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestAndreVurderinger
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.RestOpplysningsplikt
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/behandlinger/andrevurderinger")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class AndreVurderingerController(
        private val andreVurderingerService: AndreVurderingerService,
        private val fagsakService: FagsakService,
) {

    @PutMapping(path = ["/opplysningsplikt/{fagsakId}/{behandlingId}/{personResultatId}"])
    fun oppdaterOpplysningsplikt(@PathVariable fagsakId: Long,
                                 @PathVariable behandlingId: Long,
                                 @PathVariable personResultatId: Long,
                                 @RequestBody restAndreVurderinger: RestAndreVurderinger): ResponseEntity<Ressurs<RestFagsak>> {

        andreVurderingerService.oppdaterAndreVurderinger(behandlingId = behandlingId,
                                                         personResultatId = personResultatId,
                                                         andreVurderingerType = AndreVurderingerType.OPPLYSNINGSPLIKT,
                                                         resultat = restAndreVurderinger.resultat,
                                                         begrunnelse = restAndreVurderinger.begrunnelse)
        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId))
    }
}