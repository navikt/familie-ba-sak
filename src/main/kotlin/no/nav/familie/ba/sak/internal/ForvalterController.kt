package no.nav.familie.ba.sak.internal

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.OppgaveRepository
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/forvalter")
@ProtectedWithClaims(issuer = "azuread")
class ForvalterController(
    private val oppgaveRepository: OppgaveRepository,
    private val integrasjonClient: IntegrasjonClient

) {
    private val logger: Logger = LoggerFactory.getLogger(ForvalterController::class.java)

    @PostMapping(
        path = ["/ferdigstill-oppgaver"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun ferdigstillListeMedOppgaver(@RequestBody oppgaveListe: List<Long>): ResponseEntity<String> {
        var antallFeil = 0
        oppgaveListe.forEach { oppgaveId ->
            Result.runCatching {
                ferdigstillOppgave(oppgaveId)
            }.fold(
                onSuccess = { logger.info("Har ferdigstilt oppgave med oppgaveId=$oppgaveId") },
                onFailure = {
                    logger.warn("Klarte ikke å ferdigstille oppgaveId=$oppgaveId", it)
                    antallFeil = antallFeil.inc()
                }
            )
        }
        return ResponseEntity.ok("Ferdigstill oppgaver kjørt. Antall som ikke ble ferdigstilt: $antallFeil")
    }

    fun ferdigstillOppgave(oppgaveId: Long) {
        integrasjonClient.ferdigstillOppgave(oppgaveId)
        oppgaveRepository.findByGsakId(oppgaveId.toString()).also {
            if (it != null && !it.erFerdigstilt) {
                it.erFerdigstilt = true
                oppgaveRepository.saveAndFlush(it)
            }
        }
    }
}
