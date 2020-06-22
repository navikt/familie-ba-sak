package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.RessursUtils.frontendFeil
import no.nav.familie.ba.sak.common.RessursUtils.illegalState
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(Throwable::class)
    fun handleThrowable(throwable: Throwable): ResponseEntity<Ressurs<Nothing>> {
        return illegalState((throwable.cause?.message ?: throwable.message).toString(), throwable)
    }

    @ExceptionHandler(Feil::class)
    fun handleFeil(feil: Feil): ResponseEntity<Ressurs<Nothing>> {
        return frontendFeil(feil)
    }
}