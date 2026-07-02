package no.nav.familie.ba.sak.config

import io.micrometer.core.instrument.Metrics
import jakarta.servlet.http.HttpServletRequest
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
import no.nav.familie.ba.sak.common.lesRessurs
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBServiceException
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonException
import no.nav.familie.kontrakter.felles.Ressurs
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
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.io.EOFException
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.net.SocketException
import java.nio.channels.ClosedChannelException
import java.time.format.DateTimeParseException

@ControllerAdvice
class ApiExceptionHandler {
    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    private val nettverksfeilTeller =
        NettverksfeilType.entries.associateWith {
            Metrics.counter("nettverksfeil.klientavbrudd", "type", it.metrikknavn)
        }

    @ExceptionHandler(IOException::class, ClosedChannelException::class, EOFException::class)
    fun handleNettverksfeil(
        e: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<Ressurs<Nothing>> {
        val cause = NestedExceptionUtils.getMostSpecificCause(e)
        val type = NettverksfeilType.fraException(cause)

        nettverksfeilTeller[type]?.increment()
        logger.info(
            "Nettverksfeil av type=${type.metrikknavn} url=${request.method} ${request.requestURI} melding=${cause.message}",
        )

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            Ressurs.failure(frontendFeilmelding = "Tilkoblingen ble brutt"),
        )
    }

    @ExceptionHandler(RolleTilgangskontrollFeil::class)
    fun handleRolleTilgangskontrollFeil(rolleTilgangskontrollFeil: RolleTilgangskontrollFeil): ResponseEntity<Ressurs<Nothing>> = rolleTilgangResponse(rolleTilgangskontrollFeil)

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(exception: NoResourceFoundException): ResponseEntity<Ressurs<Nothing>> {
        logger.info("Fant ikke ressurs for request=${exception.resourcePath}", exception.resourcePath)

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            Ressurs.failure(
                frontendFeilmelding = "Fant ikke ressurs for request=${exception.resourcePath}",
            ),
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ResponseEntity<Ressurs<Nothing>> {
        val mostSpecificCause = NestedExceptionUtils.getMostSpecificCause(exception)

        return illegalState(mostSpecificCause.message.toString(), mostSpecificCause)
    }

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
    fun handleForbidden(forbiddenException: HttpClientErrorException.Forbidden): ResponseEntity<Ressurs<Nothing>> {
        val melding =
            lesRessurs(forbiddenException)?.melding
                ?: NestedExceptionUtils.getMostSpecificCause(forbiddenException).message
                ?: "Ikke tilgang"

        return forbidden(melding)
    }

    @ExceptionHandler(HttpClientErrorException.Unauthorized::class)
    fun handleUnauhtorized(): ResponseEntity<Ressurs<Nothing>> {
        logger.info("Fikk 401 Unauthorized")
        return unauthorized("Unauthorized")
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

enum class NettverksfeilType(
    val metrikknavn: String,
) {
    BROKEN_PIPE("broken_pipe"),
    CLOSED_CHANNEL("closed_channel"),
    CONNECTION_RESET("connection_reset"),
    EOF("eof"),
    UKJENT("ukjent"),
    ;

    companion object {
        fun fraException(e: Throwable): NettverksfeilType =
            when {
                e is ClosedChannelException -> CLOSED_CHANNEL
                e is EOFException -> EOF
                e is IOException && e.message?.lowercase()?.contains("broken pipe") == true -> BROKEN_PIPE
                e is SocketException && e.message?.lowercase()?.contains("connection reset") == true -> CONNECTION_RESET
                else -> UKJENT
            }
    }
}
