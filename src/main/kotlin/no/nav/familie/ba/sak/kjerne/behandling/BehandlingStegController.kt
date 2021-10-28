package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.RessursUtils
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/behandlinger")
@ProtectedWithClaims(issuer = "azuread")
class BehandlingStegController(
    private val behandlingService: BehandlingService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val stegService: StegService,
    private val tilgangService: TilgangService
) {

    @PostMapping(
        path = ["/{behandlingId}/registrere-søknad-og-hent-persongrunnlag"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun registrereSøknadOgHentPersongrunnlag(
        @PathVariable behandlingId: Long,
        @RequestBody restRegistrerSøknad: RestRegistrerSøknad
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val behandling = behandlingService.hent(behandlingId = behandlingId)

        stegService.håndterSøknad(behandling = behandling, restRegistrerSøknad = restRegistrerSøknad)
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandling.id)))
    }

    @PostMapping(path = ["/{behandlingId}/valider"])
    fun validerVilkårsvurdering(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val behandling = behandlingService.hent(behandlingId)
        stegService.håndterVilkårsvurdering(behandling)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandling.id)))
    }

    @PostMapping(path = ["/{behandlingId}/steg/behandlingsresultat"])
    fun utledBehandlingsresultat(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val behandling = behandlingService.hent(behandlingId)
        stegService.håndterBehandlingsresultat(behandling)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandling.id)))
    }

    @PostMapping(path = ["/{behandlingId}/tilbakekreving"])
    fun lagreTilbakekrevingOgGåVidereTilNesteSteg(
        @PathVariable behandlingId: Long,
        @RequestBody restTilbakekreving: RestTilbakekreving
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {

        val behandling = behandlingService.hent(behandlingId)
        stegService.håndterVurderTilbakekreving(behandling, restTilbakekreving)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandling.id)))
    }

    @PostMapping(path = ["/{behandlingId}/send-til-beslutter"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun sendTilBeslutter(
        @PathVariable fagsakId: Long,
        @RequestParam behandlendeEnhet: String
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val behandling = behandlingService.hentAktivForFagsak(fagsakId)
            ?: return RessursUtils.notFound("Fant ikke behandling på fagsak $fagsakId")

        stegService.håndterSendTilBeslutter(behandling, behandlendeEnhet)
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandling.id)))
    }

    @PostMapping(path = ["/{behandlingId}/iverksett-vedtak"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun iverksettVedtak(
        @PathVariable fagsakId: Long,
        @RequestBody restBeslutningPåVedtak: RestBeslutningPåVedtak
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.BESLUTTER,
            handling = "iverksette vedtak"
        )

        val behandling = behandlingService.hentAktivForFagsak(fagsakId)
            ?: return RessursUtils.notFound("Fant ikke behandling på fagsak $fagsakId")

        stegService.håndterBeslutningForVedtak(behandling, restBeslutningPåVedtak)
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandling.id)))
    }

    @PutMapping(path = ["{behandlingId}/henlegg"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun henleggBehandlingOgSendBrev(
        @PathVariable(name = "behandlingId") behandlingId: Long,
        @RequestBody henleggInfo: RestHenleggBehandlingInfo
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "henlegge behandling"
        )

        val behandling = behandlingService.hent(behandlingId)
        stegService.håndterHenleggBehandling(behandling, henleggInfo)
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandling.id)))
    }
}