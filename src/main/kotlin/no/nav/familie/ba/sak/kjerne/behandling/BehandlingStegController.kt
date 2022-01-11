package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.TEKNISK_VEDLIKEHOLD_HENLEGGELSE
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils.validerBehandlingIkkeSendtTilEksterneTjenester
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils.validerhenleggelsestype
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
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
@RequestMapping("/api/behandlinger/{behandlingId}/steg")
@ProtectedWithClaims(issuer = "azuread")
class BehandlingStegController(
    private val behandlingService: BehandlingService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val stegService: StegService,
    private val tilgangService: TilgangService,
    private val featureToggleService: FeatureToggleService,
) {

    @PostMapping(
        path = ["registrer-søknad"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun registrereSøknadOgHentPersongrunnlag(
        @PathVariable behandlingId: Long,
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

    @PostMapping(path = ["vilkårsvurdering"])
    fun validerVilkårsvurdering(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "vurdere vilkårsvurdering"
        )

        val behandling = behandlingService.hent(behandlingId)
        stegService.håndterVilkårsvurdering(behandling)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandling.id)))
    }

    @PostMapping(path = ["behandlingsresultat"])
    fun utledBehandlingsresultat(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "vurdere behandlingsresultat"
        )

        val behandling = behandlingService.hent(behandlingId)
        stegService.håndterBehandlingsresultat(behandling)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandling.id)))
    }

    @PostMapping(path = ["tilbakekreving"])
    fun lagreTilbakekrevingOgGåVidereTilNesteSteg(
        @PathVariable behandlingId: Long,
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

    @PostMapping(path = ["send-til-beslutter"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun sendTilBeslutter(
        @PathVariable behandlingId: Long,
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

    @PostMapping(path = ["iverksett-vedtak"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun iverksettVedtak(
        @PathVariable behandlingId: Long,
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

    @PutMapping(path = ["henlegg"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun henleggBehandlingOgSendBrev(
        @PathVariable behandlingId: Long,
        @RequestBody henleggInfo: RestHenleggBehandlingInfo
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "henlegge behandling"
        )

        val behandling = behandlingService.hent(behandlingId)

        validerhenleggelsestype(
            henleggÅrsak = henleggInfo.årsak,
            tekniksVedlikeholdToggel = featureToggleService.isEnabled(TEKNISK_VEDLIKEHOLD_HENLEGGELSE),
            behandlingId = behandling.id,
        )

        validerBehandlingIkkeSendtTilEksterneTjenester(behandling = behandling)

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
    FØDSELSHENDELSE_UGYLDIG_UTFALL("Behandlingen er automatisk henlagt"),
    TEKNISK_VEDLIKEHOLD("Teknisk vedlikehold");

    fun tilBehandlingsresultat() = when (this) {
        FEILAKTIG_OPPRETTET -> BehandlingResultat.HENLAGT_FEILAKTIG_OPPRETTET
        SØKNAD_TRUKKET -> BehandlingResultat.HENLAGT_SØKNAD_TRUKKET
        FØDSELSHENDELSE_UGYLDIG_UTFALL -> BehandlingResultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE
        TEKNISK_VEDLIKEHOLD -> BehandlingResultat.HENLAGT_TEKNISK_VEDLIKEHOLD
    }
}
