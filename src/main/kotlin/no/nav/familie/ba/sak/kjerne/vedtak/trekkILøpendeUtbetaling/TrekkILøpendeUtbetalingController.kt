package no.nav.familie.ba.sak.kjerne.vedtak.trekkILøpendeUtbetaling

import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
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
    private val service: TrekkILøpendeUtbetalingService
) {
    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun leggTilTrekkILøpendeUtbetaling(
        @RequestBody trekkILøpendeUtbetaling: RestTrekkILøpendeUtbetaling
    ):
        ResponseEntity<Ressurs<Long>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "legg til trekk i løpende utbetaling"
        )
        return ResponseEntity(Ressurs.success(service.leggTilTrekkILøpendeUtbetaling(trekkILøpendeUtbetaling)), HttpStatus.CREATED)
    }

    @PutMapping(produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterTrekkILøpendeUtbetaling(
        @RequestBody trekkILøpendeUtbetaling: RestTrekkILøpendeUtbetaling
    ): ResponseEntity<Ressurs<RestTrekkILøpendeUtbetaling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "oppdater trekk i løpende utbetaling"
        )
        return ResponseEntity(Ressurs.success(service.oppdaterTrekkILøpendeUtbetaling(trekkILøpendeUtbetaling)), HttpStatus.OK)
    }

    @DeleteMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun fjernTrekkILøpendeUtbetaling(
        @RequestBody identifikator: TrekkILøpendeBehandlingRestIdentifikator
    ) {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "Fjerner trekk i løpende utbetaling"
        )
        service.fjernTrekkILøpendeUtbetaling(identifikator)
    }

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentTrekkILøpendeUtbetalinger(): ResponseEntity<Ressurs<List<RestTrekkILøpendeUtbetaling>>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hente trekk i løpende utbetalinger"
        )
        return ResponseEntity.ok(Ressurs.success(service.hentTrekkILøpendeUtbetalinger()))
    }
}
