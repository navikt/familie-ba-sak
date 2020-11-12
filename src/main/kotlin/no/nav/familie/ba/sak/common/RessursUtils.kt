package no.nav.familie.ba.sak.common

import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

object RessursUtils {

    private val LOG = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun <T> unauthorized(errorMessage: String): ResponseEntity<Ressurs<T>> =
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Ressurs.failure(errorMessage))

    fun <T> notFound(errorMessage: String): ResponseEntity<Ressurs<T>> =
            errorResponse(HttpStatus.NOT_FOUND, errorMessage, null)

    fun <T> badRequest(errorMessage: String, throwable: Throwable?): ResponseEntity<Ressurs<T>> =
            errorResponse(HttpStatus.BAD_REQUEST, errorMessage, throwable)

    fun <T> forbidden(errorMessage: String): ResponseEntity<Ressurs<T>> =
            ikkeTilgangResponse(HttpStatus.FORBIDDEN, errorMessage, null)

    fun <T> illegalState(errorMessage: String, throwable: Throwable): ResponseEntity<Ressurs<T>> =
            errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage, throwable)

    fun <T> funksjonellFeil(funksjonellFeil: FunksjonellFeil): ResponseEntity<Ressurs<T>> = funksjonellErrorResponse(
            funksjonellFeil)

    fun <T> frontendFeil(feil: Feil, throwable: Throwable?): ResponseEntity<Ressurs<T>> = frontendErrorResponse(feil, throwable)

    fun <T> ok(data: T): ResponseEntity<Ressurs<T>> = ResponseEntity.ok(Ressurs.success(data))

    private fun <T> errorResponse(httpStatus: HttpStatus,
                                  errorMessage: String,
                                  throwable: Throwable?): ResponseEntity<Ressurs<T>> {
        val className = if (throwable != null) "[${throwable::class.java.name}] " else ""

        secureLogger.error("$className En feil har oppstått: $errorMessage", throwable)
        LOG.error("$className En feil har oppstått: $errorMessage")
        return ResponseEntity.status(httpStatus).body(Ressurs.failure(errorMessage))
    }

    private fun <T> ikkeTilgangResponse(httpStatus: HttpStatus,
                                  errorMessage: String,
                                  throwable: Throwable?): ResponseEntity<Ressurs<T>> {
        val className = if (throwable != null) "[${throwable::class.java.name}] " else ""

        secureLogger.warn("$className Saksbehandler har ikke tilgang: $errorMessage", throwable)
        LOG.warn("$className Saksbehandler har ikke tilgang: $errorMessage")
        return ResponseEntity.status(httpStatus).body(Ressurs.ikkeTilgang(errorMessage))
    }

    private fun <T> frontendErrorResponse(feil: Feil, throwable: Throwable?): ResponseEntity<Ressurs<T>> {
        val className = if (throwable != null) "[${throwable::class.java.name}] " else ""

        secureLogger.error("$className En håndtert feil har oppstått(${feil.httpStatus}): " +
                           "${feil.frontendFeilmelding}, ${feil.stackTrace}", throwable)
        LOG.error("$className En håndtert feil har oppstått(${feil.httpStatus}): ${feil.message} ")

        return ResponseEntity.status(feil.httpStatus).body(Ressurs.failure(
                frontendFeilmelding = feil.frontendFeilmelding,
                errorMessage = feil.message.toString()
        ))
    }

    private fun <T> funksjonellErrorResponse(funksjonellFeil: FunksjonellFeil): ResponseEntity<Ressurs<T>> {
        val className = if (funksjonellFeil.throwable != null) "[${funksjonellFeil.throwable!!::class.java.name}] " else ""

        LOG.info("$className En funksjonell feil har oppstått(${funksjonellFeil.httpStatus}): ${funksjonellFeil.message} ")

        return ResponseEntity.status(funksjonellFeil.httpStatus).body(Ressurs.funksjonellFeil(
                frontendFeilmelding = funksjonellFeil.frontendFeilmelding,
                melding = funksjonellFeil.melding
        ))
    }


    fun lagFrontendMelding(tittel: String, feilmeldinger: List<String>): String {
        var melding = tittel
        feilmeldinger.forEach {
            melding = melding.plus("\n${it}")
        }
        return melding
    }
}

inline fun <reified T> assertGenerelleSuksessKriterier(it: Ressurs<T>?) {
    val status = it?.status ?: error("Finner ikke ressurs")
    if (status == Ressurs.Status.SUKSESS && it.data == null) error("Ressurs har status suksess, men mangler data")
}
