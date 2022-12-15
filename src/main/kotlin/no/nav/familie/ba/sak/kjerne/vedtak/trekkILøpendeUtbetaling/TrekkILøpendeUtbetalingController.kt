package no.nav.familie.ba.sak.kjerne.vedtak.trekkILøpendeUtbetaling

import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
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
@RequestMapping("/api/trekk-i-loepende-utbetaling")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class TrekkILøpendeUtbetalingController(
    private val tilgangService: TilgangService,
    private val trekkILøpendeUtbetalingService: TrekkILøpendeUtbetalingService,
    private val utvidetBehandlingService: UtvidetBehandlingService
) {
    @PostMapping(path = ["behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun leggTilTrekkILøpendeUtbetaling(
        @PathVariable behandlingId: Long,
        @RequestBody trekkILøpendeUtbetaling: RestTrekkILøpendeUtbetaling
    ):
        ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "legg til trekk i løpende utbetaling"
        )

        trekkILøpendeUtbetalingService.leggTilTrekkILøpendeUtbetaling(trekkILøpendeUtbetaling = trekkILøpendeUtbetaling, behandlingId = behandlingId)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    @PutMapping(path = ["behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Transactional
    fun oppdaterTrekkILøpendeUtbetaling(
        @PathVariable behandlingId: Long,
        @RequestBody trekkILøpendeUtbetaling: RestTrekkILøpendeUtbetaling
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "oppdater trekk i løpende utbetaling"
        )

        trekkILøpendeUtbetalingService.oppdaterTrekkILøpendeUtbetaling(trekkILøpendeUtbetaling)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    @DeleteMapping(path = ["/{id}"])
    fun fjernTrekkILøpendeUtbetaling(
        @PathVariable id: Long
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "Fjerner trekk i løpende utbetaling"
        )
        val behandlingId = trekkILøpendeUtbetalingService.hentTrekkILøpendeUtbetaling(id = id).behandlingId
        trekkILøpendeUtbetalingService.fjernTrekkILøpendeUtbetaling(id = id, behandlingId = behandlingId)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    @GetMapping(path = ["behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentTrekkILøpendeUtbetalinger(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<List<RestTrekkILøpendeUtbetaling>?>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hente trekk i løpende utbetalinger"
        )
        return ResponseEntity.ok(Ressurs.success(trekkILøpendeUtbetalingService.hentTrekkILøpendeUtbetalinger(behandlingId = behandlingId)))
    }
}
