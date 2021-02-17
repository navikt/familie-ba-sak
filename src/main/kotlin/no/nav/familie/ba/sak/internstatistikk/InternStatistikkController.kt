package no.nav.familie.ba.sak.internstatistikk

import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.Ressurs
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
        val internstatistikk = InternStatistikkResponse(antallFagsakerTotalt = internStatistikkService.finnAntallFagsakerTotalt(),
                                                        antallFagsakerLøpende = internStatistikkService.finnAntallFagsakerLøpende(),
                                                        antallBehandlingerIkkeFerdigstilt = internStatistikkService.finnAntallBehandlingerIkkeErAvsluttet())
        return ResponseEntity.ok(Ressurs.Companion.success(internstatistikk))
    }

    companion object {

        val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}

data class InternStatistikkResponse(
        val antallFagsakerTotalt: Long,
        val antallFagsakerLøpende: Long,
        val antallBehandlingerIkkeFerdigstilt: Long,
)
