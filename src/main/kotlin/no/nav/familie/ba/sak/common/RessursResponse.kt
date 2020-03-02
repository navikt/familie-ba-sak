package no.nav.familie.ba.sak.common

import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

object RessursResponse {
    fun <T> notFound(errorMessage: String): ResponseEntity<Ressurs<T>> =
            errorResponse(HttpStatus.NOT_FOUND, errorMessage)

    fun <T> badRequest(errorMessage: String): ResponseEntity<Ressurs<T>> =
            errorResponse(HttpStatus.BAD_REQUEST, errorMessage)

    fun <T> forbidden(errorMessage: String): ResponseEntity<Ressurs<T>> =
            errorResponse(HttpStatus.FORBIDDEN, errorMessage)

    fun <T> errorResponse(notFound: HttpStatus, errorMessage: String): ResponseEntity<Ressurs<T>> {
        return ResponseEntity.status(notFound).body(Ressurs.failure(errorMessage))
    }
}