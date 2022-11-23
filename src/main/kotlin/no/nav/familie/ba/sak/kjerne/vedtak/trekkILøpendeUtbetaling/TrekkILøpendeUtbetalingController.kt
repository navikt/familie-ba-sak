package no.nav.familie.ba.sak.kjerne.vedtak.trekkILøpendeUtbetaling

import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
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

    @PostMapping
    fun leggTilTrekkILøpendeUtbetaling(
        @RequestBody trekkILøpendeUtbetaling: RestTrekkILøpendeUtbetaling
    ) {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "legg til trekk i løpende utbetaling"
        )
        service.leggTilTrekkILøpendeUtbetaling(trekkILøpendeUtbetaling)
    }

    @DeleteMapping
    fun fjernTrekkILøpendeUtbetaling(
        @RequestBody trekkILøpendeUtbetaling: RestTrekkILøpendeUtbetaling
    ) {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "Fjerner trekk i løpende utbetaling"
        )
        service.fjernTrekkILøpendeUtbetaling(trekkILøpendeUtbetaling)
    }

    @GetMapping
    fun hentTrekkILøpendeUtbetalinger() {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hente trekk i løpende utbetalinger"
        )
        service.hentTrekkILøpendeUtbetalinger()
    }
}
