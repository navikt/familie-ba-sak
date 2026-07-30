package no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs

import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth

@RestController
@RequestMapping("/api/satsendring-eos")
class SatsendringEøsController(
    private val startSatsendringEøs: StartSatsendringEøs,
    private val tilgangService: TilgangService,
) {
    @PostMapping(path = ["/kjor"])
    fun opprettSatsendringEøsForRelevanteFagsaker(
        @RequestBody request: SatsendringEøsRequest,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            BehandlerRolle.FORVALTER,
            "Trigge EØS-satsendring for alle relevante fagsaker",
        )
        val fagsakIder =
            startSatsendringEøs.opprettSatsendringEøsTaskerForRelevanteFagsaker(
                utbetalingsland = request.utbetalingsland,
                satsTidspunkt = request.satsTidspunkt,
            )
        return ResponseEntity.ok(Ressurs.success("Trigget EØS-satsendring for ${fagsakIder.size} fagsaker"))
    }

    @PostMapping(path = ["/kjor/{fagsakId}"])
    fun opprettSatsendringEøsForFagsak(
        @PathVariable fagsakId: Long,
        @RequestBody request: SatsendringEøsRequest,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            BehandlerRolle.FORVALTER,
            "Trigge EØS-satsendring for én fagsak",
        )
        startSatsendringEøs.opprettSatsendringEøsTaskForFagsak(
            fagsakId = fagsakId,
            utbetalingsland = request.utbetalingsland,
            satsTidspunkt = request.satsTidspunkt,
        )
        return ResponseEntity.ok(Ressurs.success("Trigget EØS-satsendring for fagsak $fagsakId"))
    }
}

data class SatsendringEøsRequest(
    val utbetalingsland: String,
    val satsTidspunkt: YearMonth,
)
