package no.nav.familie.ba.sak.statistikk.internstatistikk.søknadsstatistikk

import no.nav.familie.ba.sak.common.RessursUtils
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.behandling.domene.SøknadsstatistikkForPeriode
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeParseException

@RestController
@RequestMapping("/api/internstatistikk")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SøknadsstatistikkController(
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService,
) {

    @GetMapping(path = ["antallSoknader"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentSøknadsstatistikkForPeriode(
        @RequestParam fom: String?,
        @RequestParam tom: String?,
    ): ResponseEntity<Ressurs<SøknadsstatistikkForPeriode>> {
        return try {
            val fomDato = fom?.let { LocalDate.parse(it) } ?: LocalDate.now().minusMonths(4).withDayOfMonth(1)
            val tomDato = tom?.let { LocalDate.parse(it) } ?: fomDato.plusMonths(4).minusDays(1)

            logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter søknadsstatistikk for periode $fomDato $tomDato")
            RessursUtils.ok(behandlingSøknadsinfoService.hentSøknadsstatistikk(fomDato, tomDato))
        } catch (e: DateTimeParseException) {
            RessursUtils.badRequest("Ugyldig dato", e)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(SøknadsstatistikkController::class.java)
    }
}
