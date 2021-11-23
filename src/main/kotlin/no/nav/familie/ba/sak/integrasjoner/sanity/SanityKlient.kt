package no.nav.familie.ba.sak.kjerne.dokument

import no.nav.familie.ba.sak.kjerne.dokument.domene.RestSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityBegrunnelse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

data class SanityRespons<T>(
    val ms: Int,
    val query: String,
    val result: List<T>
)

@Component
class SanityKlient(
    @Value("\${SANITY_FAMILIE_API_URL}") private val sanityFamilieApi: String,
    @Qualifier("medProxy") val restOperations: RestOperations,
) {

    fun hentSanityBegrunnelser(): List<SanityBegrunnelse> {
        val parameters = java.net.URLEncoder.encode(hentDokumenter, "utf-8")
        val url = URI.create("$sanityFamilieApi?query=$parameters")
        logger.info("Henter begrunnelser fra sanity")

        val response = restOperations.exchange(
            url,
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<SanityRespons<RestSanityBegrunnelse>>() {},
        )
        val restSanityBegrunnelser = response.body?.result ?: error("Klarte ikke hente begrunnelsene fra familie-brev.")
        return restSanityBegrunnelser.map { it.tilSanityBegrunnelse() }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(BrevKlient::class.java)
    }
}
