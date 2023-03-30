package no.nav.familie.ba.sak.kjerne.behandling.settpåvent

import no.nav.familie.ba.sak.ekstern.restDomene.RestSettPåVent
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingId
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
    private val utvidetBehandlingService: UtvidetBehandlingService
) {
    @PostMapping(path = ["{behandlingId}"])
    fun settBehandlingPåVent(
        @PathVariable behandlingId: Long,
        @RequestBody restSettPåVent: RestSettPåVent
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Sett behandling på vent"
        )
        val parsetBehandlingId = BehandlingId(behandlingId)
        settPåVentService.settBehandlingPåVent(parsetBehandlingId, restSettPåVent.frist, restSettPåVent.årsak)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = parsetBehandlingId)))
    }

    @PutMapping(path = ["{behandlingId}"])
    fun oppdaterSettBehandlingPåVent(
        @PathVariable behandlingId: Long,
        @RequestBody restSettPåVent: RestSettPåVent
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Sett behandling på vent"
        )
        val parsetBehandlingId = BehandlingId(behandlingId)
        settPåVentService.oppdaterSettBehandlingPåVent(parsetBehandlingId, restSettPåVent.frist, restSettPåVent.årsak)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = parsetBehandlingId)))
    }

    @PutMapping(path = ["{behandlingId}/fortsettbehandling"])
    fun gjenopptaBehandling(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Sett behandling på vent"
        )
        val parsetBehandlingId = BehandlingId(behandlingId)
        settPåVentService.gjenopptaBehandling(parsetBehandlingId)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = parsetBehandlingId)))
    }
}
