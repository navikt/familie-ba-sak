package no.nav.familie.ba.sak.kjerne.minside

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.kontrakter.felles.Fødselsnummer
import no.nav.familie.sikkerhet.EksternBrukerUtils
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/minside/barnetrygd")
@RequiredIssuers(
    ProtectedWithClaims(
        issuer = EksternBrukerUtils.ISSUER_TOKENX,
        claimMap = ["acr=Level4"],
    ),
)
@Validated
class MinSideBarnetrygdController(
    private val minSideBarnetrygdService: MinSideBarnetrygdService,
) {
    private val logger = LoggerFactory.getLogger(MinSideBarnetrygdController::class.java)

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentMinSideBarnetrygd(): ResponseEntity<HentMinSideBarnetrygdDto> {
        val fnrFraToken: String?
        try {
            fnrFraToken = EksternBrukerUtils.hentFnrFraToken()
        } catch (exception: Exception) {
            logger.error("Feil ved henting av fnr fra token. Se SecureLogs for detaljer")
            secureLogger.error("Feil ved henting av fnr fra token.", exception)
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(HentMinSideBarnetrygdDto.Feil("Mangler tilgang."))
        }

        val fnr: Fødselsnummer?
        try {
            fnr = Fødselsnummer(fnrFraToken)
        } catch (exception: Exception) {
            logger.error("Ugydlig fnr ved henting av min side barnetrygd. Se SecureLogs for detaljer")
            secureLogger.error("Ugydlig fnr=$fnrFraToken ved henting av min side barnetrygd", exception)
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(HentMinSideBarnetrygdDto.Feil("Ugydlig fødselsnummer."))
        }

        try {
            val minSideBarnetrygd = minSideBarnetrygdService.hentMinSideBarnetrygd(fnr)
            return ResponseEntity
                .status(HttpStatus.OK)
                .body(HentMinSideBarnetrygdDto.Suksess.opprettFraDomene(minSideBarnetrygd))
        } catch (exception: Exception) {
            logger.error("Henting av barnetrygd for min side feilet. Se SecureLog for detaljer")
            secureLogger.error("Henting av barnetrygd for min side feilet for ugyldig fnr=${fnr.verdi}.", exception)
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(HentMinSideBarnetrygdDto.Feil("En ukjent feil oppstod."))
        }
    }
}
