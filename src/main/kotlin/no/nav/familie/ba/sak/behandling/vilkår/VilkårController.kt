package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.RestNyttVilkår
import no.nav.familie.ba.sak.behandling.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.behandling.restDomene.RestVedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/vilkaarsvurdering")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VilkårController(
        private val vilkårService: VilkårService,
        private val behandlingService: BehandlingService,
        private val vedtakService: VedtakService,
        private val stegService: StegService,
        private val fagsakService: FagsakService
) {

    @PutMapping(path = ["/{behandlingId}/{vilkaarId}"])
    fun endreVilkår(@PathVariable behandlingId: Long,
                    @PathVariable vilkaarId: Long,
                    @RequestBody restPersonResultat: RestPersonResultat): ResponseEntity<Ressurs<RestFagsak>> {
        val behandling = behandlingService.hent(behandlingId)
        vilkårService.endreVilkår(behandlingId = behandling.id,
                                  vilkårId = vilkaarId,
                                  restPersonResultat = restPersonResultat)

        settStegOgSlettUtbetalingBegrunnelser(behandling.id)
        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id))
    }

    @DeleteMapping(path = ["/{behandlingId}/{vilkaarId}"])
    fun slettVilkår(@PathVariable behandlingId: Long,
                    @PathVariable vilkaarId: Long,
                    @RequestBody personIdent: String): ResponseEntity<Ressurs<RestFagsak>> {
        val behandling = behandlingService.hent(behandlingId)
        vilkårService.deleteVilkår(behandlingId = behandling.id,
                                   vilkårId = vilkaarId,
                                   personIdent = personIdent)

        settStegOgSlettUtbetalingBegrunnelser(behandling.id)
        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id))
    }

    @PostMapping(path = ["/{behandlingId}"])
    fun nyttVilkår(@PathVariable behandlingId: Long, @RequestBody restNyttVilkår: RestNyttVilkår):
            ResponseEntity<Ressurs<RestFagsak>> {
        val behandling = behandlingService.hent(behandlingId)
        vilkårService.postVilkår(behandling.id, restNyttVilkår)

        settStegOgSlettUtbetalingBegrunnelser(behandlingId)
        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id))
    }

    @PostMapping(path = ["/{behandlingId}/valider"])
    fun validerVilkårsvurdering(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        val behandling = behandlingService.hent(behandlingId)
        stegService.håndterVilkårsvurdering(behandling)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id))
    }

    @GetMapping(path = ["/vilkaarsbegrunnelser"])
    fun hentTeksterForVilkårsbegrunnelser(): ResponseEntity<Ressurs<Map<VedtakBegrunnelseType, List<RestVedtakBegrunnelse>>>> {
        return ResponseEntity.ok(Ressurs.success(VilkårsvurderingUtils.hentVilkårsbegrunnelser()))
    }

    /**
     * Når et vilkår vurderes (endres) vil begrunnelsene satt på dette vilkåret resettes
     */
    @Transactional
    fun settStegOgSlettUtbetalingBegrunnelser(behandlingId: Long) {
        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(behandlingId = behandlingId, steg = StegType.VILKÅRSVURDERING)
        vedtakService.slettUtbetalingBegrunnelser(behandlingId)
    }
}



