package no.nav.familie.ba.sak.kjerne.dokument

import no.nav.familie.ba.sak.kjerne.dokument.domene.RestSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityBegrunnelse
import no.nav.familie.http.client.AbstractRestClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URI

data class SanityRespons<T>(
    val ms: Int,
    val query: String,
    val result: List<T>
)

@Component
class SanityKlient(
    @Value("\${SANITY_FAMILIE_API_URL}") private val sanityFamilieApi: String,
    restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "sanity") {

    fun hentSanityBegrunnelser(): List<SanityBegrunnelse> {
        val parameters = java.net.URLEncoder.encode(hentDokumenter, "utf-8")
        val url = URI.create("$sanityFamilieApi?query=$parameters")
        logger.info("Henter begrunnelser fra sanity")

        val response = getForEntity<SanityRespons<RestSanityBegrunnelse>>(url)
        val restSanityBegrunnelser = response.result
        return restSanityBegrunnelser.map { it.tilSanityBegrunnelse() }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(BrevKlient::class.java)
    }
}
