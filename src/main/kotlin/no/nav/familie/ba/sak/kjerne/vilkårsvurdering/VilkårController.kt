package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.AnnenVurderingDto
import no.nav.familie.ba.sak.ekstern.restDomene.NyttVilkårDto
import no.nav.familie.ba.sak.ekstern.restDomene.PersonResultatDto
import no.nav.familie.ba.sak.ekstern.restDomene.RestSlettVilkår
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.RestVedtakBegrunnelseTilknyttetVilkår
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Fødselsnummer
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
    private val personidentService: PersonidentService,
    private val tilgangService: TilgangService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
) {
    @PutMapping(path = ["/{behandlingId}/{vilkaarId}"])
    fun endreVilkår(
        @PathVariable behandlingId: Long,
        @PathVariable vilkaarId: Long,
        @RequestBody personResultatDto: PersonResultatDto,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "endre vilkår",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)

        vilkårService.endreVilkår(
            behandlingId = behandlingId,
            vilkårId = vilkaarId,
            personResultatDto = personResultatDto,
        )
        tilbakestillBehandlingService.resettStegVedEndringPåVilkår(behandlingId)
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    @PutMapping(path = ["/{behandlingId}/annenvurdering/{annenVurderingId}"])
    fun endreAnnenVurdering(
        @PathVariable behandlingId: Long,
        @PathVariable annenVurderingId: Long,
        @RequestBody annenVurderingDto: AnnenVurderingDto,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Annen vurdering",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)

        annenVurderingService.endreAnnenVurdering(
            behandlingId = behandlingId,
            annenVurderingId = annenVurderingId,
            annenVurderingDto = annenVurderingDto,
        )

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    @DeleteMapping(path = ["/{behandlingId}/{vilkaarId}"])
    fun slettVilkårsperiode(
        @PathVariable behandlingId: Long,
        @PathVariable vilkaarId: Long,
        @RequestBody personIdent: String,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        // Valider persionIdent
        Fødselsnummer(personIdent)

        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.DELETE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "slette vilkårsperiode",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)

        val aktør = personidentService.hentAktør(personIdent)

        vilkårService.deleteVilkårsperiode(
            behandlingId = behandlingId,
            vilkårId = vilkaarId,
            aktør = aktør,
        )

        tilbakestillBehandlingService.resettStegVedEndringPåVilkår(behandlingId)
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    @DeleteMapping(path = ["/{behandlingId}/vilkaar"])
    fun slettVilkår(
        @PathVariable behandlingId: Long,
        @RequestBody restSlettVilkår: RestSlettVilkår,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.DELETE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "slette vilkår",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)

        vilkårService.deleteVilkår(behandlingId, restSlettVilkår)

        tilbakestillBehandlingService.resettStegVedEndringPåVilkår(behandlingId)
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    @PostMapping(path = ["/{behandlingId}"])
    fun nyttVilkår(
        @PathVariable behandlingId: Long,
        @RequestBody nyttVilkårDto: NyttVilkårDto,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "legge til vilkår",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)

        vilkårService.postVilkår(behandlingId, nyttVilkårDto)

        tilbakestillBehandlingService.resettStegVedEndringPåVilkår(behandlingId)
        return ResponseEntity.ok(
            Ressurs.success(
                utvidetBehandlingService
                    .lagRestUtvidetBehandling(behandlingId = behandlingId),
            ),
        )
    }

    @GetMapping(path = ["/vilkaarsbegrunnelser"])
    fun hentTeksterForVilkårsbegrunnelser(): ResponseEntity<Ressurs<Map<VedtakBegrunnelseType, List<RestVedtakBegrunnelseTilknyttetVilkår>>>> = ResponseEntity.ok(Ressurs.success(vilkårsvurderingService.hentVilkårsbegrunnelser()))
}
