package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.common.EksternTjenesteFeil
import no.nav.familie.ba.sak.common.EksternTjenesteFeilException
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.PdlNotFoundException
import no.nav.familie.ba.sak.common.RessursUtils.forbidden
import no.nav.familie.ba.sak.common.RessursUtils.frontendFeil
import no.nav.familie.ba.sak.common.RessursUtils.funksjonellFeil
import no.nav.familie.ba.sak.common.RessursUtils.illegalState
import no.nav.familie.ba.sak.common.RessursUtils.rolleTilgangResponse
import no.nav.familie.ba.sak.common.RessursUtils.unauthorized
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonException
import no.nav.familie.ba.sak.integrasjoner.infotrygd.KanIkkeMigrereException
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.core.NestedExceptionUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.client.HttpClientErrorException
import java.io.PrintWriter
import java.io.StringWriter

@ControllerAdvice
class ApiExceptionHandler {

    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    fun handleThrowable(jwtTokenUnauthorizedException: JwtTokenUnauthorizedException): ResponseEntity<Ressurs<Nothing>> {
        return unauthorized("Unauthorized")
    }

    @ExceptionHandler(RolleTilgangskontrollFeil::class)
    fun handleRolleTilgangskontrollFeil(rolleTilgangskontrollFeil: RolleTilgangskontrollFeil): ResponseEntity<Ressurs<Nothing>> {
        return rolleTilgangResponse(rolleTilgangskontrollFeil)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ResponseEntity<Ressurs<Nothing>> {
        val mostSpecificCause = NestedExceptionUtils.getMostSpecificCause(exception)

        return illegalState(mostSpecificCause.message.toString(), mostSpecificCause)
    }

    @ExceptionHandler(HttpClientErrorException.Forbidden::class)
    fun handleForbidden(foriddenException: HttpClientErrorException.Forbidden): ResponseEntity<Ressurs<Nothing>> {
        val mostSpecificCause = NestedExceptionUtils.getMostSpecificCause(foriddenException)

        return forbidden(mostSpecificCause.message ?: "Ikke tilgang")
    }

    @ExceptionHandler(IntegrasjonException::class)
    fun handleIntegrasjonException(integrasjonException: IntegrasjonException): ResponseEntity<Ressurs<Nothing>> {
        return illegalState(integrasjonException.message.toString(), integrasjonException)
    }

    @ExceptionHandler(PdlNotFoundException::class)
    fun handlePdlNotFoundException(feil: PdlNotFoundException): ResponseEntity<Ressurs<Nothing>> {
        logger.warn("Finner ikke personen i PDL")
        return ResponseEntity.ok()
            .body(Ressurs.failure(frontendFeilmelding = "Fant ikke person"))
    }

    @ExceptionHandler(Feil::class)
    fun handleFeil(feil: Feil): ResponseEntity<Ressurs<Nothing>> {
        val mostSpecificCause =
            if (feil.throwable != null) NestedExceptionUtils.getMostSpecificCause(feil.throwable!!) else null

        return frontendFeil(feil, mostSpecificCause)
    }

    @ExceptionHandler(FunksjonellFeil::class)
    fun handleFunksjonellFeil(funksjonellFeil: FunksjonellFeil): ResponseEntity<Ressurs<Nothing>> {
        return funksjonellFeil(funksjonellFeil)
    }

    @ExceptionHandler(EksternTjenesteFeilException::class)
    fun handleEksternTjenesteFeil(feil: EksternTjenesteFeilException): ResponseEntity<EksternTjenesteFeil> {
        val mostSpecificThrowable =
            if (feil.throwable != null) NestedExceptionUtils.getMostSpecificCause(feil.throwable) else null
        feil.eksternTjenesteFeil.exception =
            if (mostSpecificThrowable != null) "[${mostSpecificThrowable::class.java.name}] " else null

        if (mostSpecificThrowable != null) {
            val sw = StringWriter()
            feil.printStackTrace(PrintWriter(sw))
            feil.eksternTjenesteFeil.stackTrace = sw.toString()
        }

        secureLogger.info("$feil")
        logger.info("Feil ekstern tjeneste: path:${feil.eksternTjenesteFeil.path} status:${feil.eksternTjenesteFeil.status} exception:${feil.eksternTjenesteFeil.exception}")

        return ResponseEntity.status(feil.eksternTjenesteFeil.status).body(feil.eksternTjenesteFeil)
    }

    @ExceptionHandler(KanIkkeMigrereException::class)
    fun handleKanIkkeMigrereException(feil: KanIkkeMigrereException): ResponseEntity<Ressurs<String>> {
        val mostSpecificThrowable =
            if (feil.throwable != null) NestedExceptionUtils.getMostSpecificCause(feil.throwable!!) else null

        val feilmelding = if (feil.melding != null) feil.melding else feil.feiltype.beskrivelse

        val ressurs: Ressurs<String> = Ressurs.failure<String>(
            errorMessage = feilmelding,
            frontendFeilmelding = feilmelding,
            error = mostSpecificThrowable
        ).copy(data = feil.feiltype.name)
        secureLogger.warn("Feil ved migrering. feiltype=${feil.feiltype} melding=$feilmelding", feil.throwable)
        logger.warn("Feil ved migrering. feiltype=${feil.feiltype} melding=$feilmelding")

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ressurs
        )
    }
}
