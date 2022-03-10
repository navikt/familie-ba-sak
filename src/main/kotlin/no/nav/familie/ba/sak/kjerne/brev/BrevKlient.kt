package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.kallEksternTjeneste
import no.nav.familie.ba.sak.kjerne.brev.domene.RestSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
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
        val uri = URI.create("$familieBrevUri/api/$sanityDataset/dokument/$målform/${brev.mal.apiNavn}/pdf")

        secureLogger.info("Kaller familie brev($uri) med data ${brev.data.toBrevString()}")
        val response = kallEksternTjeneste("famile-brev", uri, "Hente pdf for vedtaksbrev") {
            restTemplate.postForEntity<ByteArray>(uri, brev.data)
        }

        return response.body ?: throw Feil("Klarte ikke generere brev med familie-brev")
    }

    @Cacheable("begrunnelsestekster-for-nedtreksmeny", cacheManager = "shortCache")
    fun hentSanityBegrunnelser(): List<SanityBegrunnelse> {
        val uri = URI.create("$familieBrevUri/ba-sak/begrunnelser")

        val restSanityBegrunnelser =
            kallEksternTjeneste("famile-brev", uri, "Henter begrunnelser fra sanity via familie brev") {
                restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    null,
                    object : ParameterizedTypeReference<List<RestSanityBegrunnelse>>() {},
                ).body ?: throw Feil("Klarte ikke hente begrunnelsene fra familie-brev.")
            }

        return restSanityBegrunnelser.map { it.tilSanityBegrunnelse() }
    }

    @Cacheable("begrunnelsestekst", cacheManager = "shortCache")
    fun hentBegrunnelsestekst(begrunnelseData: BegrunnelseData): String {
        val uri = URI.create("$familieBrevUri/ba-sak/begrunnelser/${begrunnelseData.apiNavn}/tekst/")
        secureLogger.info("Kaller familie brev($uri) med data $begrunnelseData")

        val response = kallEksternTjeneste("famile-brev", uri, "Henter begrunnelsestekst") {
            restTemplate.postForEntity<String>(uri, begrunnelseData)
        }

        return response.body ?: error("Klarte ikke å hente begrunnelsestekst fra familie-brev.")
    }

    companion object {

        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
