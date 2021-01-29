package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.RessursUtils.forbidden
import no.nav.familie.ba.sak.common.RessursUtils.frontendFeil
import no.nav.familie.ba.sak.common.RessursUtils.funksjonellFeil
import no.nav.familie.ba.sak.common.RessursUtils.illegalState
import no.nav.familie.ba.sak.common.RessursUtils.rolleTilgangResponse
import no.nav.familie.ba.sak.common.RessursUtils.unauthorized
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.core.NestedExceptionUtils
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import javax.validation.ConstraintViolationException

@ControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    fun handleThrowable(jwtTokenUnauthorizedException: JwtTokenUnauthorizedException): ResponseEntity<Ressurs<Nothing>> {
        return unauthorized("Unauthorized")
    }

    @ExceptionHandler(RolleTilgangskontrollFeil::class)
    fun handleFunksjonellFeil(rolleTilgangskontrollFeil: RolleTilgangskontrollFeil): ResponseEntity<Ressurs<Nothing>> {
        return rolleTilgangResponse(rolleTilgangskontrollFeil)
    }

    //Disse kastes av FagsaktilgangConstraint og persontilgangConstraint
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleThrowable(constraintViolationException: ConstraintViolationException): ResponseEntity<Ressurs<Nothing>> {
        return forbidden("Ikke tilgang")
    }

    @ExceptionHandler(Throwable::class)
    fun handleThrowable(throwable: Throwable): ResponseEntity<Ressurs<Nothing>> {
        val mostSpecificThrowable = NestedExceptionUtils.getMostSpecificCause(throwable)

        return illegalState(mostSpecificThrowable.message.toString(), mostSpecificThrowable)
    }

    @ExceptionHandler(Feil::class)
    fun handleFunksjonellFeil(feil: Feil): ResponseEntity<Ressurs<Nothing>> {
        val mostSpecificThrowable =
                if (feil.throwable != null) NestedExceptionUtils.getMostSpecificCause(feil.throwable!!) else null

        return frontendFeil(feil, mostSpecificThrowable)
    }

    @ExceptionHandler(FunksjonellFeil::class)
    fun handleFunksjonellFeil(funksjonellFeil: FunksjonellFeil): ResponseEntity<Ressurs<Nothing>> {
        return funksjonellFeil(funksjonellFeil)
    }
}