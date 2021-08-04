package no.nav.familie.ba.sak.integrasjoner.statistikk

import no.nav.familie.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI

@Service
@Deprecated("Midlertidig kode")
class StatistikkClient(@Value("\${FAMILIE_STATISTIKK_URL}") val baseUri: URI,
                       @Qualifier("jwtBearer") val restTemplate: RestOperations)
    : AbstractRestClient(restTemplate, "statistikk") {

    fun hentSakStatistikk(offset: Long): String {
        val uri = URI.create("$baseUri/statistikk/sak/$offset")

        return getForEntity(uri)
    }

    fun hentBehandlingStatistikk(offset: Long): String {
        val uri = URI.create("$baseUri/statistikk/behandling/$offset")

        return getForEntity(uri)
    }

}
