package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.sikkerhet.validering.BehandlingstilgangConstraint
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
@RequestMapping("/api/behandlinger/steg")
@ProtectedWithClaims(issuer = "azuread")
class BehandlingStegController(
    private val behandlingService: BehandlingService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val stegService: StegService,
    private val tilgangService: TilgangService
) {

    @PostMapping(
        path = ["/{behandlingId}/registrer-søknad"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun registrereSøknadOgHentPersongrunnlag(
        @PathVariable @BehandlingstilgangConstraint behandlingId: Long,
        @RequestBody restRegistrerSøknad: RestRegistrerSøknad
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "registrere søknad"
        )

        val behandling = behandlingService.hent(behandlingId = behandlingId)

        stegService.håndterSøknad(behandling = behandling, restRegistrerSøknad = restRegistrerSøknad)
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandling.id)))
    }

    @PostMapping(path = ["/{behandlingId}/vilkårsvurdering"])
    fun validerVilkårsvurdering(@PathVariable @BehandlingstilgangConstraint behandlingId: Long): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "vurdere vilkårsvurdering"
        )

        val behandling = behandlingService.hent(behandlingId)
        stegService.håndterVilkårsvurdering(behandling)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandling.id)))
    }

    @PostMapping(path = ["/{behandlingId}/behandlingsresultat"])
    fun utledBehandlingsresultat(@PathVariable @BehandlingstilgangConstraint behandlingId: Long): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "vurdere behandlingsresultat"
        )

        val behandling = behandlingService.hent(behandlingId)
        stegService.håndterBehandlingsresultat(behandling)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandling.id)))
    }

    @PostMapping(path = ["/{behandlingId}/tilbakekreving"])
    fun lagreTilbakekrevingOgGåVidereTilNesteSteg(
        @PathVariable @BehandlingstilgangConstraint behandlingId: Long,
        @RequestBody restTilbakekreving: RestTilbakekreving?
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "vurdere tilbakekreving"
        )

        val behandling = behandlingService.hent(behandlingId)
        stegService.håndterVurderTilbakekreving(behandling, restTilbakekreving)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandling.id)))
    }

    @PostMapping(path = ["/{behandlingId}/send-til-beslutter"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun sendTilBeslutter(
        @PathVariable @BehandlingstilgangConstraint behandlingId: Long,
        @RequestParam behandlendeEnhet: String
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "foreslå vedtak"
        )

        val behandling = behandlingService.hent(behandlingId)

        stegService.håndterSendTilBeslutter(behandling, behandlendeEnhet)
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandling.id)))
    }

    @PostMapping(path = ["/{behandlingId}/iverksett-vedtak"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun iverksettVedtak(
        @PathVariable @BehandlingstilgangConstraint behandlingId: Long,
        @RequestBody restBeslutningPåVedtak: RestBeslutningPåVedtak
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.BESLUTTER,
            handling = "iverksette vedtak"
        )

        val behandling = behandlingService.hent(behandlingId)

        stegService.håndterBeslutningForVedtak(behandling, restBeslutningPåVedtak)
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandling.id)))
    }

    @PutMapping(path = ["{behandlingId}/henlegg"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun henleggBehandlingOgSendBrev(
        @PathVariable @BehandlingstilgangConstraint behandlingId: Long,
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

class RestHenleggBehandlingInfo(
    val årsak: HenleggÅrsak,
    val begrunnelse: String
)

enum class HenleggÅrsak(val beskrivelse: String) {
    SØKNAD_TRUKKET("Søknad trukket"),
    FEILAKTIG_OPPRETTET("Behandling feilaktig opprettet"),
    FØDSELSHENDELSE_UGYLDIG_UTFALL("Behandlingen er automatisk henlagt");

    fun tilBehandlingsresultat() = when (this) {
        FEILAKTIG_OPPRETTET -> BehandlingResultat.HENLAGT_FEILAKTIG_OPPRETTET
        SØKNAD_TRUKKET -> BehandlingResultat.HENLAGT_SØKNAD_TRUKKET
        FØDSELSHENDELSE_UGYLDIG_UTFALL -> BehandlingResultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE
    }
}
