package no.nav.familie.ba.sak.kjerne.dokument

import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import java.net.URI

@Component
class BrevKlient(
    @Value("\${FAMILIE_BREV_API_URL}") private val familieBrevUri: String,
    @Value("\${SANITY_DATASET}") private val sanityDataset: String,
    private val restTemplate: RestTemplate
) {

    fun genererBrev(målform: String, brev: Brev): ByteArray {
        val url = URI.create("$familieBrevUri/api/$sanityDataset/dokument/$målform/${brev.mal.apiNavn}/pdf")
        secureLogger.info("Kaller familie brev($url) med data ${brev.data.toBrevString()}")
        logger.info("Kaller familie brev med url: $url}")
        val response = restTemplate.postForEntity<ByteArray>(url, brev.data)
        return response.body ?: error("Klarte ikke generere brev med familie-brev")
    }

    @Cacheable("begrunnelsestekster-for-nedtreksmeny")
    fun hentSanityBegrunnelser(): List<SanityBegrunnelse> {
        val url = URI.create("$familieBrevUri/ba-sak/begrunnelser")
        logger.info("Henter begrunnelser fra sanity")
        val response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<List<RestSanityBegrunnelse>>() {},
        )
        val restSanityBegrunnelser = response.body ?: error("Klarte ikke hente begrunnelsene fra familie-brev.")
        return restSanityBegrunnelser.map { it.tilSanityBegrunnelse() }
    }

    @Cacheable("begrunnelsestekst")
    fun hentBegrunnelsestekst(begrunnelseData: BegrunnelseData): String {
        val url = URI.create("$familieBrevUri/ba-sak/begrunnelser/${begrunnelseData.apiNavn}/tekst/")
        secureLogger.info("Kaller familie brev($url) med data $begrunnelseData")
        logger.info("Kaller familie brev med url: $url}")
        val response = restTemplate.postForEntity<String>(url, begrunnelseData)
        return response.body ?: error("Klarte ikke å hente begrunnelsestekst fra familie-brev.")
    }

    companion object {

        private val secureLogger = LoggerFactory.getLogger("secureLogger")
        private val logger = LoggerFactory.getLogger(BrevKlient::class.java)
    }
}
