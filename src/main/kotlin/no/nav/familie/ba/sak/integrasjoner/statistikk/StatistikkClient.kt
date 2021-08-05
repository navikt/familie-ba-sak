package no.nav.familie.ba.sak.integrasjoner.statistikk

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.log.NavHttpHeaders
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import java.net.URI

@Service
@Deprecated("Midlertidig kode")
class StatistikkClient(@Value("\${FAMILIE_STATISTIKK_URL}") val baseUri: URI,
                       @Qualifier("jwtBearer") val restTemplate: RestOperations)
    : AbstractRestClient(restTemplate, "statistikk") {

    fun hentSakStatistikk(offset: Long): String {
        val uri = URI.create("$baseUri/statistikk/sak/$offset")

        return try {
            getForEntity(uri, httpHeaders())
        } catch (e: Exception) {
            if (e is HttpClientErrorException) {
                logger.error("Kall mot statistikk feilet: httpkode: ${e.statusCode}, body ${e.responseBodyAsString} ", e)
            }
            throw e
        }
    }

    fun hentBehandlingStatistikk(offset: Long): String {
        val uri = URI.create("$baseUri/statistikk/behandling/$offset")

        return getForEntity(uri, httpHeaders())
    }

    private fun httpHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            add(HttpHeaders.CONTENT_TYPE, "application/json")
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(StatistikkClient::class.java)
    }

}
