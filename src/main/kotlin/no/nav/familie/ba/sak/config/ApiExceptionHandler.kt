package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.RessursUtils.illegalState
import no.nav.familie.ba.sak.common.TekniskFeil
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
    fun handleException(throwable: Throwable): ResponseEntity<Ressurs<Nothing>> {
        secureLogger.error("En feil har oppstått: ${throwable.message}, ${throwable.stackTrace}")
        logger.info("En feil har oppstått: ${throwable.message} ")

        return illegalState((throwable.cause?.message ?: throwable.message).toString(), throwable)
    }

    @ExceptionHandler(FunksjonellFeil::class)
    fun handleFunksjonellFeil(funksjonellFeil: FunksjonellFeil): ResponseEntity<Ressurs<Nothing>> {
        secureLogger.error("En funksjonell feil har oppstått(${funksjonellFeil.httpStatus}): " +
                           "${funksjonellFeil.funksjonellFeilmelding}, ${funksjonellFeil.stackTrace}")
        logger.info("En funksjonell feil har oppstått(${funksjonellFeil.httpStatus}): ${funksjonellFeil.message} ")

        return ResponseEntity.status(funksjonellFeil.httpStatus).body(Ressurs(
                status = Ressurs.Status.FEILET,
                funksjonellFeilmelding = funksjonellFeil.funksjonellFeilmelding,
                melding = funksjonellFeil.message.toString(),
                data = null,
                stacktrace = null
        ))
    }

    @ExceptionHandler(TekniskFeil::class)
    fun handleTekniskFeil(tekniskFeil: TekniskFeil): ResponseEntity<Ressurs<Nothing>> {
        secureLogger.error("En teknisk feil har oppstått(${tekniskFeil.httpStatus}): " +
                           "${tekniskFeil.message}, ${tekniskFeil.stackTrace}")
        logger.info("En teknsik feil har oppstått(${tekniskFeil.httpStatus}): ${tekniskFeil.message} ")

        return ResponseEntity.status(tekniskFeil.httpStatus).body(Ressurs(
                status = Ressurs.Status.FEILET,
                melding = tekniskFeil.message.toString(),
                data = null,
                stacktrace = null
        ))
    }
}