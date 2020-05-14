package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.RessursUtils.illegalState
import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ApiExceptionHandler {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @ExceptionHandler(Throwable::class)
    fun handleThrowable(throwable: Throwable): ResponseEntity<Ressurs<Nothing>> {
        secureLogger.error("En feil har oppstått: ${throwable.message}, ${throwable.stackTrace}")
        logger.info("En feil har oppstått: ${throwable.message} ")

        return illegalState((throwable.cause?.message ?: throwable.message).toString(), throwable)
    }

    @ExceptionHandler(Feil::class)
    fun handleFeil(feil: Feil): ResponseEntity<Ressurs<Nothing>> {
        secureLogger.error("En håndtert feil har oppstått(${feil.httpStatus}): " +
                           "${feil.frontendFeilmelding}, ${feil.stackTrace}")
        logger.info("En håndtert feil har oppstått(${feil.httpStatus}): ${feil.message} ")

        return ResponseEntity.status(feil.httpStatus).body(Ressurs.failure(
                frontendFeilmelding = feil.frontendFeilmelding,
                errorMessage = feil.message.toString()
        ))
    }
}