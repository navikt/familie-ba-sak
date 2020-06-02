package no.nav.familie.ba.sak.client

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.familie.http.client.AbstractRestClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class Norg2RestClient(@Value("\${NORG2_BASE_URL}") private val norg2BaseUrl: URI,
                      restTemplate: RestOperations)
    : AbstractRestClient(restTemplate, "norg2") {

    private val kontaktinformasjonUri: UriComponentsBuilder =
            UriComponentsBuilder.fromUri(norg2BaseUrl).pathSegment(PATH_HENT_ENHET)

    fun hentEnhet(enhet: String?): Enhet {
        try {
            LoggerFactory.getLogger(this::class.java)
                    .info("Henter enhet fra ${kontaktinformasjonUri.pathSegment("$enhet").build().toUri()}")
            return getForEntity(kontaktinformasjonUri.pathSegment("$enhet").build().toUri())
        } catch (e: Exception) {
            throw RuntimeException("Feil ved henting av enhet fra NORG2", e)
        }
    }


    companion object {
        private const val PATH_HENT_ENHET = "api/v1/enhet"
    }
}

@JsonInclude
data class Enhet(
        val enhetId: Long,
        val navn: String
)