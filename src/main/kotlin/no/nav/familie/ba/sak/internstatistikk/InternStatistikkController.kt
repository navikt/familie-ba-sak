package no.nav.familie.ba.sak.internstatistikk

import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
@Validated
class InternStatistikkController(
        private val internStatistikkService: InternStatistikkService
) {

    @GetMapping(path = ["internstatistikk"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentAntallFagsakerOpprettet(): ResponseEntity<Ressurs<InternStatistikkResponse>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter internstatistikk")
        val antallFagsakerTotalt = internStatistikkService.finnAntallFagsakerTotalt()
        val antallFagsakerLøpende = internStatistikkService.finnAntallFagsakerLøpende()
        val antallBehandlingerIkkeAvsluttet = internStatistikkService.finnAntallBehandlingerIkkeErAvsluttet()
        val res = InternStatistikkResponse(antallFagsakerTotalt = antallFagsakerTotalt,
                                           antallFagsakerLøpende = antallFagsakerLøpende,
                                           antallBehandlingerIkkeFerdigstilt = antallBehandlingerIkkeAvsluttet)
        return ResponseEntity.ok(Ressurs.Companion.success(res))
    }

    companion object {

        val logger: Logger = LoggerFactory.getLogger(this::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}

data class InternStatistikkResponse(
        val antallFagsakerTotalt: Long,
        val antallFagsakerLøpende: Long,
        val antallBehandlingerIkkeFerdigstilt: Long,
)
