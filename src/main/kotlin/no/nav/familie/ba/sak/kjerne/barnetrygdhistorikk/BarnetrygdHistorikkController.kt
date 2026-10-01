package no.nav.familie.ba.sak.kjerne.barnetrygdhistorikk

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/barnetrygdhistorikk")
class BarnetrygdHistorikkController(
    private val tilgangService: TilgangService,
    private val barnetrygdHistorikkService: BarnetrygdHistorikkService,
) {
    @Operation(
        summary = "Sjekker om søker har hatt utbetaling",
        description =
            "Returnerer true dersom fagsaken har andeler på vedtatte behandlinger, " +
                "eller dersom søker har stønader i Infotrygd. Andeler med 0 kroner i utbetaling teller også. " +
                "Pågående og henlagte behandlinger teller ikke. Returnerer false når ingen av delene finnes.",
    )
    @ApiResponse(responseCode = "200", description = "Resultatet returneres som Ressurs<Boolean>.")
    @GetMapping(path = ["/fagsak/{fagsakId}/soker-har-hatt-invilget_barnetrygd"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun harSøkerHattUtbetaling(
        @PathVariable fagsakId: Long,
    ): ResponseEntity<Ressurs<Boolean>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SYSTEM,
            handling = "svarer om det har vært utbetaling til aktør på fagsak",
        )
        return ResponseEntity.ok(Ressurs.success(barnetrygdHistorikkService.harSøkerHattInnvilgetBarnetrygd(fagsakId)))
    }
}
