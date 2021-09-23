package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.ekstern.restDomene.RestAnnenVurdering
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestNyttVilkår
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestVedtakBegrunnelseTilknyttetVilkår
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vilkaarsvurdering")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VilkårController(
        private val vilkårService: VilkårService,
        private val annenVurderingService: AnnenVurderingService,
        private val behandlingService: BehandlingService,
        private val vedtakService: VedtakService,
        private val stegService: StegService,
        private val fagsakService: FagsakService,
        private val tilgangService: TilgangService,
        private val vilkårsvurderingService: VilkårsvurderingService,
) {

    @PutMapping(path = ["/{behandlingId}/{vilkaarId}"])
    fun endreVilkår(@PathVariable behandlingId: Long,
                    @PathVariable vilkaarId: Long,
                    @RequestBody restPersonResultat: RestPersonResultat): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "endre vilkår")

        val behandling = behandlingService.hent(behandlingId)
        vilkårService.endreVilkår(behandlingId = behandling.id,
                                  vilkårId = vilkaarId,
                                  restPersonResultat = restPersonResultat)
        vedtakService.settStegSlettTilbakekreving(behandling.id)
        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id))
    }

    @PutMapping(path = ["/{behandlingId}/annenvurdering/{annenVurderingId}"])
    fun endreAnnenVurdering(@PathVariable behandlingId: Long,
                            @PathVariable annenVurderingId: Long,
                            @RequestBody restAnnenVurdering: RestAnnenVurdering): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "Annen vurdering")

        val behandling = behandlingService.hent(behandlingId)
        annenVurderingService.endreAnnenVurdering(annenVurderingId = annenVurderingId,
                                                  restAnnenVurdering = restAnnenVurdering)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id))
    }

    @DeleteMapping(path = ["/{behandlingId}/{vilkaarId}"])
    fun slettVilkår(@PathVariable behandlingId: Long,
                    @PathVariable vilkaarId: Long,
                    @RequestBody personIdent: String): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "slette vilkår")

        val behandling = behandlingService.hent(behandlingId)
        vilkårService.deleteVilkår(behandlingId = behandling.id,
                                   vilkårId = vilkaarId,
                                   personIdent = personIdent)

        vedtakService.settStegSlettTilbakekreving(behandling.id)
        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id))
    }

    @PostMapping(path = ["/{behandlingId}"])
    fun nyttVilkår(@PathVariable behandlingId: Long, @RequestBody restNyttVilkår: RestNyttVilkår):
            ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "legge til vilkår")

        val behandling = behandlingService.hent(behandlingId)
        vilkårService.postVilkår(behandling.id, restNyttVilkår)

        vedtakService.settStegSlettTilbakekreving(behandlingId)
        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id))
    }

    @PostMapping(path = ["/{behandlingId}/valider"])
    fun validerVilkårsvurdering(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        val behandling = behandlingService.hent(behandlingId)
        stegService.håndterVilkårsvurdering(behandling)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id))
    }

    @GetMapping(path = ["/vilkaarsbegrunnelser"])
    fun hentTeksterForVilkårsbegrunnelser(): ResponseEntity<Ressurs<Map<VedtakBegrunnelseType, List<RestVedtakBegrunnelseTilknyttetVilkår>>>> {
        return ResponseEntity.ok(Ressurs.success(vilkårsvurderingService.hentVilkårsbegrunnelser()))
    }
}



