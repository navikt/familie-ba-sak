package no.nav.familie.ba.sak.kjerne.vedtak.sammensattKontrollsak

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.ekstern.restDomene.RestOpprettSammensattKontrollsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestSammensattKontrollsak
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
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
@RequestMapping("/api/sammensatt-kontrollsak")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SammensattKontrollsakController(
    val tilgangService: TilgangService,
    val sammensattKontrollsakService: SammensattKontrollsakService,
    val utvidetBehandlingService: UtvidetBehandlingService,
    val unleashService: UnleashNextMedContextService,
) {
    private final val ikkeTilgangFeilmelding =
        "Du har ikke tilgang til å opprette og endre sammensatte kontrollsaker. Dette krever spesialtilgang."

    @GetMapping(
        path = ["/{behandlingId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentSammensattKontrollsak(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<RestSammensattKontrollsak?>> {
        if (!unleashService.isEnabled(FeatureToggleConfig.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)) {
            throw FunksjonellFeil(melding = ikkeTilgangFeilmelding)
        }

        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Hent SammensattKontrollsak",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId = behandlingId)

        val sammensattKontrollsak = sammensattKontrollsakService.finnSammensattKontrollsak(behandlingId = behandlingId)

        return ResponseEntity.ok(Ressurs.success(sammensattKontrollsak?.tilRestSammensattKontrollsak()))
    }

    @PostMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun opprettSammensattKontrollsak(
        @RequestBody restOpprettSammensattKontrollsak: RestOpprettSammensattKontrollsak,
    ): ResponseEntity<Ressurs<RestSammensattKontrollsak>> {
        if (!unleashService.isEnabled(FeatureToggleConfig.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)) {
            throw FunksjonellFeil(melding = ikkeTilgangFeilmelding)
        }

        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Opprett SammensattKontrollsak",
        )
        tilgangService.validerKanRedigereBehandling(restOpprettSammensattKontrollsak.behandlingId)

        val sammensattKontrollsak = sammensattKontrollsakService.opprettSammensattKontrollsak(restOpprettSammensattKontrollsak = restOpprettSammensattKontrollsak)

        return ResponseEntity.ok(Ressurs.success(sammensattKontrollsak.tilRestSammensattKontrollsak()))
    }

    @PutMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun oppdaterSammensattKontrollsak(
        @RequestBody restSammensattKontrollsak: RestSammensattKontrollsak,
    ): ResponseEntity<Ressurs<RestSammensattKontrollsak>> {
        if (!unleashService.isEnabled(FeatureToggleConfig.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)) {
            throw FunksjonellFeil(melding = ikkeTilgangFeilmelding)
        }

        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppdater SammensattKontrollsak",
        )
        tilgangService.validerKanRedigereBehandling(restSammensattKontrollsak.behandlingId)

        val sammensattKontrollsak = sammensattKontrollsakService.oppdaterSammensattKontrollsak(restSammensattKontrollsak = restSammensattKontrollsak)

        return ResponseEntity.ok(Ressurs.success(sammensattKontrollsak.tilRestSammensattKontrollsak()))
    }

    @DeleteMapping(
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun slettSammensattKontrollsak(
        @RequestBody restSammensattKontrollsak: RestSammensattKontrollsak,
    ): ResponseEntity<Ressurs<Long>> {
        if (!unleashService.isEnabled(FeatureToggleConfig.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)) {
            throw FunksjonellFeil(melding = ikkeTilgangFeilmelding)
        }

        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Slett SammensattKontrollsak",
        )
        tilgangService.validerKanRedigereBehandling(restSammensattKontrollsak.behandlingId)

        sammensattKontrollsakService.slettSammensattKontrollsak(restSammensattKontrollsak.id)

        return ResponseEntity.ok(Ressurs.success(restSammensattKontrollsak.id))
    }
}
