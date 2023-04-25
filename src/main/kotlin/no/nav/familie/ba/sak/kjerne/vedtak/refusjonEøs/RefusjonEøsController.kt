package no.nav.familie.ba.sak.kjerne.vedtak.refusjonEøs

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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/refusjon-eøs")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class RefusjonEøsController(
    private val tilgangService: TilgangService,
    private val refusjonEøsService: RefusjonEøsService,
    private val utvidetBehandlingService: UtvidetBehandlingService
) {
    @PostMapping(
        path = ["behandling/{behandlingId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun leggTilRefusjonEøsPeriode(
        @PathVariable behandlingId: Long,
        @RequestBody refusjonEøs: RestRefusjonEøs
    ):
        ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "legg til periode med refusjon EØS"
        )

        refusjonEøsService.leggTilRefusjonEøsPeriode(
            refusjonEøs = refusjonEøs,
            behandlingId = behandlingId
        )

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    @PutMapping(
        path = ["behandling/{behandlingId}/periode/{id}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun oppdaterRefusjonEøsPeriode(
        @PathVariable behandlingId: Long,
        @PathVariable id: Long,
        @RequestBody refusjonEøs: RestRefusjonEøs
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "oppdater periode med refusjon EØS"
        )

        refusjonEøsService.oppdatertRefusjonEøsPeriode(refusjonEøs = refusjonEøs, id = id)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    @DeleteMapping(path = ["behandling/{behandlingId}/periode/{id}"])
    fun fjernRefusjonEøsPeriode(
        @PathVariable behandlingId: Long,
        @PathVariable id: Long
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "fjerner periode med refusjon EØS"
        )
        refusjonEøsService.fjernRefusjonEøsPeriode(id = id, behandlingId = behandlingId)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    @GetMapping(path = ["behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentRefusjonEøsPerioder(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<List<RestRefusjonEøs>?>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hente refusjon EØS for behandling"
        )
        return ResponseEntity.ok(Ressurs.success(refusjonEøsService.hentRefusjonEøsPerioder(behandlingId = behandlingId)))
    }
}
