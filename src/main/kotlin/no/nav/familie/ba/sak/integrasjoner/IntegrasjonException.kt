package no.nav.familie.ba.sak.integrasjoner

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.client.RestClientResponseException
import java.net.URI
import kotlin.text.Charsets.UTF_8

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
class IntegrasjonException : RuntimeException {
    private var responseBody: ByteArray? = null

    constructor(msg: String?) : super(msg)
    constructor(msg: String?, throwable: Throwable?, uri: URI?, ident: String?) : super(msg, throwable) {
        var message = ""
        if (throwable is RestClientResponseException) {
            message = throwable.responseBodyAsString
            responseBody = throwable.responseBodyAsByteArray
        }
        secureLogger.info("Ukjent feil ved integrasjon mot {}. ident={} {} {}", uri, ident, message, throwable)
        logger.warn("Ukjent feil ved integrasjon mot '{}'.", uri)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IntegrasjonTjeneste::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}