package no.nav.familie.ba.sak.common

import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

object RessursUtils {
    private val LOG = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun <T> notFound(errorMessage: String): ResponseEntity<Ressurs<T>> =
            errorResponse(HttpStatus.NOT_FOUND, errorMessage, null)

    fun <T> badRequest(errorMessage: String, throwable: Throwable?): ResponseEntity<Ressurs<T>> =
            errorResponse(HttpStatus.BAD_REQUEST, errorMessage, throwable)

    fun <T> forbidden(errorMessage: String): ResponseEntity<Ressurs<T>> =
            errorResponse(HttpStatus.FORBIDDEN, errorMessage, null)

    fun <T> illegalState(errorMessage: String, throwable: Throwable): ResponseEntity<Ressurs<T>> =
            errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage, throwable)

    fun <T> ok(data: T): ResponseEntity<Ressurs<T>> = ResponseEntity.ok(Ressurs.success(data))

    private fun <T> errorResponse(httpStatus: HttpStatus,
                                  errorMessage: String,
                                  throwable: Throwable?): ResponseEntity<Ressurs<T>> {
        secureLogger.info(errorMessage, throwable)
        LOG.error(errorMessage)
        return ResponseEntity.status(httpStatus).body(Ressurs.failure(errorMessage))
    }

    inline fun <reified T> assertGenerelleSuksessKriterier(it: Ressurs<T>?) {
        val status = it?.status ?: error("Finner ikke ressurs")
        if (status == Ressurs.Status.SUKSESS && it.data == null) error("Ressurs har status suksess, men mangler data")
    }

    fun lagFrontendMelding(tittel: String, feilmeldinger: List<String>): String {
        var melding = tittel
        feilmeldinger.forEach {
            melding = melding.plus("\n${it}")
        }
        return melding
    }
}