package no.nav.familie.ba.sak.kjerne.vedtak.sammensattKontrollsak

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.OpprettSammensattKontrollsakDto
import no.nav.familie.ba.sak.ekstern.restDomene.SammensattKontrollsakDto
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
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
    val featureToggleService: FeatureToggleService,
) {
    private final val ikkeTilgangFeilmelding =
        "Du har ikke tilgang til å opprette og endre sammensatte kontrollsaker. Dette krever spesialtilgang."

    @GetMapping(
        path = ["/{behandlingId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentSammensattKontrollsak(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<SammensattKontrollsakDto?>> {
        if (!featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)) {
            throw FunksjonellFeil(melding = ikkeTilgangFeilmelding)
        }

        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Hent SammensattKontrollsak",
        )
        val sammensattKontrollsak = sammensattKontrollsakService.finnSammensattKontrollsak(behandlingId = behandlingId)

        return ResponseEntity.ok(Ressurs.success(sammensattKontrollsak?.tilSammensattKontrollsakDto()))
    }

    @PostMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun opprettSammensattKontrollsak(
        @RequestBody opprettSammensattKontrollsakDto: OpprettSammensattKontrollsakDto,
    ): ResponseEntity<Ressurs<SammensattKontrollsakDto>> {
        if (!featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)) {
            throw FunksjonellFeil(melding = ikkeTilgangFeilmelding)
        }

        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Opprett SammensattKontrollsak",
        )
        tilgangService.validerKanRedigereBehandling(opprettSammensattKontrollsakDto.behandlingId)

        val sammensattKontrollsak = sammensattKontrollsakService.opprettSammensattKontrollsak(opprettSammensattKontrollsakDto = opprettSammensattKontrollsakDto)

        return ResponseEntity.ok(Ressurs.success(sammensattKontrollsak.tilSammensattKontrollsakDto()))
    }

    @PutMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun oppdaterSammensattKontrollsak(
        @RequestBody sammensattKontrollsakDto: SammensattKontrollsakDto,
    ): ResponseEntity<Ressurs<SammensattKontrollsakDto>> {
        if (!featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)) {
            throw FunksjonellFeil(melding = ikkeTilgangFeilmelding)
        }

        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppdater SammensattKontrollsak",
        )
        tilgangService.validerKanRedigereBehandling(sammensattKontrollsakDto.behandlingId)

        val sammensattKontrollsak = sammensattKontrollsakService.oppdaterSammensattKontrollsak(sammensattKontrollsakDto = sammensattKontrollsakDto)

        return ResponseEntity.ok(Ressurs.success(sammensattKontrollsak.tilSammensattKontrollsakDto()))
    }

    @DeleteMapping(
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun slettSammensattKontrollsak(
        @RequestBody sammensattKontrollsakDto: SammensattKontrollsakDto,
    ): ResponseEntity<Ressurs<Long>> {
        if (!featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_OG_ENDRE_SAMMENSATTE_KONTROLLSAKER)) {
            throw FunksjonellFeil(melding = ikkeTilgangFeilmelding)
        }

        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Slett SammensattKontrollsak",
        )
        tilgangService.validerKanRedigereBehandling(sammensattKontrollsakDto.behandlingId)

        sammensattKontrollsakService.slettSammensattKontrollsak(sammensattKontrollsakDto.id)

        return ResponseEntity.ok(Ressurs.success(sammensattKontrollsakDto.id))
    }
}
