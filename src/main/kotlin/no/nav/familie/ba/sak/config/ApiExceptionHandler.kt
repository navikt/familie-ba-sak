package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.common.EksternTjenesteFeil
import no.nav.familie.ba.sak.common.EksternTjenesteFeilException
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.MånedligValutaJusteringFeil
import no.nav.familie.ba.sak.common.PdlNotFoundException
import no.nav.familie.ba.sak.common.PdlPersonKanIkkeBehandlesIFagsystem
import no.nav.familie.ba.sak.common.RessursUtils.forbidden
import no.nav.familie.ba.sak.common.RessursUtils.frontendFeil
import no.nav.familie.ba.sak.common.RessursUtils.funksjonellFeil
import no.nav.familie.ba.sak.common.RessursUtils.illegalState
import no.nav.familie.ba.sak.common.RessursUtils.rolleTilgangResponse
import no.nav.familie.ba.sak.common.RessursUtils.unauthorized
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBServiceException
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonException
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.core.NestedExceptionUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.io.PrintWriter
import java.io.StringWriter
import java.time.format.DateTimeParseException

@ControllerAdvice
class ApiExceptionHandler {
    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    fun handleThrowable(jwtTokenUnauthorizedException: JwtTokenUnauthorizedException): ResponseEntity<Ressurs<Nothing>> = unauthorized("Unauthorized")

    @ExceptionHandler(RolleTilgangskontrollFeil::class)
    fun handleRolleTilgangskontrollFeil(rolleTilgangskontrollFeil: RolleTilgangskontrollFeil): ResponseEntity<Ressurs<Nothing>> = rolleTilgangResponse(rolleTilgangskontrollFeil)

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ResponseEntity<Ressurs<Nothing>> {
        val mostSpecificCause = NestedExceptionUtils.getMostSpecificCause(exception)

        return illegalState(mostSpecificCause.message.toString(), mostSpecificCause)
    }

    @ExceptionHandler(RessursException::class)
    fun handleRessursException(ressursException: RessursException): ResponseEntity<Ressurs<Any>> = ResponseEntity.status(ressursException.httpStatus).body(ressursException.ressurs)

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<Ressurs<Nothing>> {
        logger.info("Ikke forventet verdi på property= ${e.propertyName} med verdi=${e.value} for metode=${e.parameter}")

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            Ressurs.failure(
                frontendFeilmelding = "Klarer ikke å tyde verdi på ${e.propertyName}. Sjekk at url er riktig",
            ),
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(e: HttpMessageNotReadableException): ResponseEntity<Ressurs<Nothing>> {
        val mostSpecificCause = NestedExceptionUtils.getMostSpecificCause(e)

        return when (mostSpecificCause) {
            is DateTimeParseException -> {
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Ressurs.failure(
                        frontendFeilmelding = "Ugyldig datoformat ${mostSpecificCause.parsedString}",
                    ),
                )
            }

            is FunksjonellFeil -> {
                funksjonellFeil(mostSpecificCause)
            }

            else -> {
                illegalState(mostSpecificCause.message.toString(), mostSpecificCause)
            }
        }
    }

    @ExceptionHandler(HttpClientErrorException.Forbidden::class)
    fun handleForbidden(foriddenException: HttpClientErrorException.Forbidden): ResponseEntity<Ressurs<Nothing>> {
        val mostSpecificCause = NestedExceptionUtils.getMostSpecificCause(foriddenException)

        return forbidden(mostSpecificCause.message ?: "Ikke tilgang")
    }

    @ExceptionHandler(IntegrasjonException::class)
    fun handleIntegrasjonException(integrasjonException: IntegrasjonException): ResponseEntity<Ressurs<Nothing>> = illegalState(integrasjonException.message.toString(), integrasjonException)

    @ExceptionHandler(PdlPersonKanIkkeBehandlesIFagsystem::class)
    fun handlePdlPersonKanIkkeBehandlesIFagsystem(feil: PdlPersonKanIkkeBehandlesIFagsystem): ResponseEntity<Ressurs<Nothing>> {
        logger.warn("Person kan ikke behandles i fagsystem ${feil.årsak}")
        secureLogger.warn("Person kan ikke behandles i fagsystem", feil)
        return funksjonellFeil(feil)
    }

    @ExceptionHandler(PdlNotFoundException::class)
    fun handlePdlNotFoundException(feil: PdlNotFoundException): ResponseEntity<Ressurs<Nothing>> {
        logger.warn("Finner ikke personen i PDL")
        return ResponseEntity
            .ok()
            .body(Ressurs.failure(frontendFeilmelding = "Fant ikke person"))
    }

    @ExceptionHandler(MånedligValutaJusteringFeil::class)
    fun handleMånedligValutaJusteringFeil(feil: MånedligValutaJusteringFeil): ResponseEntity<Ressurs<Nothing>> =
        ResponseEntity.status(HttpStatus.OK).body(
            Ressurs.funksjonellFeil(
                frontendFeilmelding = feil.melding,
                melding = feil.melding,
            ),
        )

    @ExceptionHandler(ECBServiceException::class)
    fun handleECBClientException(feil: ECBServiceException): ResponseEntity<Ressurs<Nothing>> {
        logger.warn(feil.message)
        return ResponseEntity
            .internalServerError()
            .body(Ressurs.failure(frontendFeilmelding = feil.message))
    }

    @ExceptionHandler(Feil::class)
    fun handleFeil(feil: Feil): ResponseEntity<Ressurs<Nothing>> {
        val mostSpecificCause =
            if (feil.throwable != null) NestedExceptionUtils.getMostSpecificCause(feil.throwable!!) else null

        return frontendFeil(feil, mostSpecificCause)
    }

    @ExceptionHandler(FunksjonellFeil::class)
    fun handleFunksjonellFeil(funksjonellFeil: FunksjonellFeil): ResponseEntity<Ressurs<Nothing>> = funksjonellFeil(funksjonellFeil)

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

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleInputValideringFeil(valideringFeil: MethodArgumentNotValidException): ResponseEntity<Ressurs<Nothing>> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                Ressurs.failure(
                    valideringFeil.bindingResult.fieldErrors
                        .map { fieldError -> fieldError.defaultMessage }
                        .joinToString(" ,"),
                ),
            )
}
