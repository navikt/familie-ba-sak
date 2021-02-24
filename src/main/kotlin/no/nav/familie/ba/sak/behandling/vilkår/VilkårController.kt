package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.annenvurdering.AnnenVurderingService
import no.nav.familie.ba.sak.annenvurdering.AnnenVurderingType
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.*
import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
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
        private val annenVurderingService: AnnenVurderingService,
        private val behandlingService: BehandlingService,
        private val vedtakService: VedtakService,
        private val stegService: StegService,
        private val fagsakService: FagsakService,
        private val tilgangService: TilgangService,
        private val loggService: LoggService,
) {

    @PutMapping(path = ["/{behandlingId}/{vilkaarId}"])
    fun endreVilkår(@PathVariable behandlingId: Long,
                    @PathVariable vilkaarId: Long,
                    @RequestBody restPersonResultat: RestPersonResultat): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER, handling = "endre vilkår")

        val behandling = behandlingService.hent(behandlingId)
        vilkårService.endreVilkår(behandlingId = behandling.id,
                                  vilkårId = vilkaarId,
                                  restPersonResultat = restPersonResultat)

        settStegOgSlettVedtakBegrunnelser(behandling.id)
        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id))
    }

    @PutMapping(path = ["/{behandlingId}/annenvurdering/{annenVurderingId}"])
    fun endreAnnenVurdering(@PathVariable behandlingId: Long,
                        @PathVariable annenVurderingId: Long,
                        @RequestBody restAnnenVurdering: RestAnnenVurdering): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER, handling = "Annen vurdering")

        val behandling = behandlingService.hent(behandlingId)
        annenVurderingService.endreAnnenVurdering(annenVurderingId = annenVurderingId,
                                                  restAnnenVurdering = restAnnenVurdering)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id))
    }

    @DeleteMapping(path = ["/{behandlingId}/{vilkaarId}"])
    fun slettVilkår(@PathVariable behandlingId: Long,
                    @PathVariable vilkaarId: Long,
                    @RequestBody personIdent: String): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER, handling = "slette vilkår")

        val behandling = behandlingService.hent(behandlingId)
        vilkårService.deleteVilkår(behandlingId = behandling.id,
                                   vilkårId = vilkaarId,
                                   personIdent = personIdent)

        settStegOgSlettVedtakBegrunnelser(behandling.id)
        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id))
    }

    @PostMapping(path = ["/{behandlingId}"])
    fun nyttVilkår(@PathVariable behandlingId: Long, @RequestBody restNyttVilkår: RestNyttVilkår):
            ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER, handling = "legge til vilkår")

        val behandling = behandlingService.hent(behandlingId)
        vilkårService.postVilkår(behandling.id, restNyttVilkår)

        settStegOgSlettVedtakBegrunnelser(behandlingId)
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
        return ResponseEntity.ok(Ressurs.success(VilkårsvurderingUtils.hentVilkårsbegrunnelser()))
    }

    /**
     * Når et vilkår vurderes (endres) vil begrunnelsene satt på dette vilkåret resettes
     */
    private fun settStegOgSlettVedtakBegrunnelser(behandlingId: Long) {
        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(behandlingId = behandlingId,
                                                                              steg = StegType.VILKÅRSVURDERING)
        vedtakService.slettAlleVedtakBegrunnelser(behandlingId)
    }
}



