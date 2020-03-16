package no.nav.familie.ba.sak.common

import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

object RessursResponse {
    val LOG = LoggerFactory.getLogger(this::class.java)

    fun <T> notFound(errorMessage: String): ResponseEntity<Ressurs<T>> =
            errorResponse(HttpStatus.NOT_FOUND, errorMessage)

    fun <T> badRequest(errorMessage: String): ResponseEntity<Ressurs<T>> =
            errorResponse(HttpStatus.BAD_REQUEST, errorMessage)

    fun <T> forbidden(errorMessage: String): ResponseEntity<Ressurs<T>> =
            errorResponse(HttpStatus.FORBIDDEN, errorMessage)

    fun <T> illegalState(errorMessage: String): ResponseEntity<Ressurs<T>> =
            errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage)

    fun <T> ok(data: T): ResponseEntity<Ressurs<T>> = ResponseEntity.ok(Ressurs.success(data))

    private fun <T> errorResponse(notFound: HttpStatus, errorMessage: String): ResponseEntity<Ressurs<T>> {
        LOG.error(errorMessage)
        return ResponseEntity.status(notFound).body(Ressurs.failure(errorMessage))
    }
}