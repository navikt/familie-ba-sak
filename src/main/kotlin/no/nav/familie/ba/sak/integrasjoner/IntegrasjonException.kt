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
    constructor(msg: String?, e: Throwable?, uri: URI?, ident: String?) : super(msg, e) {
        var message = ""
        if (e is RestClientResponseException) {
            message = e.responseBodyAsString
            responseBody = e.responseBodyAsByteArray
        }
        secureLogger.info("Ukjent feil ved integrasjon mot {}. ident={} {} {}", uri, ident, message, e)
        logger.warn("Ukjent feil ved integrasjon mot '{}'.", uri)
    }

    val responseBodyAsString: String?
        get() = if (responseBody == null) {
            null
        } else String(responseBody!!, UTF_8)

    companion object {
        private val logger = LoggerFactory.getLogger(IntegrasjonTjeneste::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}