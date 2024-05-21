package no.nav.familie.ba.sak.kjerne.vedtak.sammensattKontrollsak

import no.nav.familie.ba.sak.ekstern.restDomene.RestOpprettSammensattKontrollsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestSammensattKontrollsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/sammensatt-kontrollsak")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SammensattKontrollsakController(val tilgangService: TilgangService, val sammensattKontrollsakService: SammensattKontrollsakService, val utvidetBehandlingService: UtvidetBehandlingService) {
    @PostMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun opprettSammensattKontrollsak(
        @RequestBody restOpprettSammensattKontrollsak: RestOpprettSammensattKontrollsak,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Opprett SammensattKontrollsak",
        )
        tilgangService.validerKanRedigereBehandling(restOpprettSammensattKontrollsak.behandlingId)

        sammensattKontrollsakService.opprettSammensattKontrollsak(restOpprettSammensattKontrollsak = restOpprettSammensattKontrollsak)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = restOpprettSammensattKontrollsak.behandlingId)))
    }

    @PutMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun oppdaterSammensattKontrollsak(
        @RequestBody restSammensattKontrollsak: RestSammensattKontrollsak,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppdater SammensattKontrollsak",
        )
        tilgangService.validerKanRedigereBehandling(restSammensattKontrollsak.behandlingId)

        sammensattKontrollsakService.oppdaterSammensattKontrollsak(restSammensattKontrollsak = restSammensattKontrollsak)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = restSammensattKontrollsak.behandlingId)))
    }

    @DeleteMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun slettSammensattKontrollsak(
        @RequestBody restSammensattKontrollsak: RestSammensattKontrollsak,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Slett SammensattKontrollsak",
        )
        tilgangService.validerKanRedigereBehandling(restSammensattKontrollsak.behandlingId)

        sammensattKontrollsakService.slettSammensattKontrollsak(restSammensattKontrollsak.id)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = restSammensattKontrollsak.behandlingId)))
    }
}
