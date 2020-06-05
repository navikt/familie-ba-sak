package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.behandling.steg.StegService
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
        private val vilkårService: VilkårService,
        private val behandlingService: BehandlingService,
        private val stegService: StegService,
        private val fagsakService: FagsakService
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

    @DeleteMapping(path = ["/{behandlingId}/{vilkaarId}"])
    fun slettVilkår(@PathVariable behandlingId: Long,
                    @PathVariable vilkaarId: Long,
                    @RequestBody personIdent: String): ResponseEntity<Ressurs<List<RestPersonResultat>>> {
        val nyVilkårsvurdering = vilkårService.deleteVilkår(behandlingId = behandlingId,
                                                           vilkårId = vilkaarId,
                                                           personIdent = personIdent)

        return ResponseEntity.ok(Ressurs.success(nyVilkårsvurdering))
    }

    @PostMapping(path = ["/{behandlingId}/valider"])
    fun validerVilkårsvurdering(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        val behandling = behandlingService.hent(behandlingId)
        stegService.håndterVilkårsvurdering(behandling)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id))
    }
}

