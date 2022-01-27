package no.nav.familie.ba.sak.kjerne.behandling.settpåvent

import no.nav.familie.ba.sak.ekstern.restDomene.RestSettPåVent
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestSettPåVent
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/sett-på-vent/")
@ProtectedWithClaims(issuer = "azuread")
class SettPåVentController(
    private val tilgangService: TilgangService,
    private val settPåVentService: SettPåVentService,
) {
    @PostMapping(path = ["{behandlingId}"])
    fun settBehandlingPåVent(
        @PathVariable behandlingId: Long,
        @RequestBody restSettPåVent: RestSettPåVent,
    ): ResponseEntity<Ressurs<RestSettPåVent>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Sett behandling på vent"
        )

        val setPåVent = settPåVentService.settbehandlingPåVent(behandlingId, restSettPåVent.frist, restSettPåVent.årsak)
        return ResponseEntity.ok(Ressurs.success(setPåVent.tilRestSettPåVent()))
    }

    @PutMapping(path = ["{behandlingId}"])
    fun oppdaterSettBehandlingPåVent(
        @PathVariable behandlingId: Long,
        @RequestBody restSettPåVent: RestSettPåVent,
    ): ResponseEntity<Ressurs<RestSettPåVent>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Sett behandling på vent"
        )

        val setPåVent =
            settPåVentService.oppdaterSettbehandlingPåVent(behandlingId, restSettPåVent.frist, restSettPåVent.årsak)
        return ResponseEntity.ok(Ressurs.success(setPåVent.tilRestSettPåVent()))
    }

    @PutMapping(path = ["{behandlingId}/fortsettbehandling"])
    fun fjernSettBehandlingPåVent(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<RestSettPåVent>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Sett behandling på vent"
        )

        val setPåVent = settPåVentService.deaktiverSettBehandlingPåVent(behandlingId)
        return ResponseEntity.ok(Ressurs.success(setPåVent.tilRestSettPåVent()))
    }
}
