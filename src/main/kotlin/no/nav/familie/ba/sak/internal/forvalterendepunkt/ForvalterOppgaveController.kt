package no.nav.familie.ba.sak.internal.forvalterendepunkt

import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/forvalter/oppgave")
class ForvalterOppgaveController(
    private val tilgangService: TilgangService,
    private val oppgaveService: OppgaveService,
) {
    private val logger: Logger = LoggerFactory.getLogger(ForvalterOppgaveController::class.java)

    @PostMapping(
        path = ["/ferdigstill-oppgaver"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun ferdigstillListeMedOppgaver(
        @RequestBody oppgaveListe: List<Long>,
    ): ResponseEntity<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Ferdigstill liste med oppgaver",
        )

        var antallFeil = 0
        oppgaveListe.forEach { oppgaveId ->
            Result
                .runCatching {
                    oppgaveService.tvingFerdigstillOppgave(oppgaveId)
                }.fold(
                    onSuccess = { logger.info("Har ferdigstilt oppgave med oppgaveId=$oppgaveId") },
                    onFailure = {
                        logger.warn("Klarte ikke å ferdigstille oppgaveId=$oppgaveId", it)
                        antallFeil = antallFeil.inc()
                    },
                )
        }
        return ResponseEntity.ok("Ferdigstill oppgaver kjørt. Antall som ikke ble ferdigstilt: $antallFeil")
    }
}
