package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.kallEksternTjeneste
import no.nav.familie.ba.sak.kjerne.brev.domene.RestSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import no.nav.familie.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URI

@Component
class BrevKlient(
    @Value("\${FAMILIE_BREV_API_URL}") private val familieBrevUri: String,
    @Value("\${SANITY_DATASET}") private val sanityDataset: String,
    restTemplate: RestTemplate
) : AbstractRestClient(restTemplate, "familie-brev") {

    fun genererBrev(målform: String, brev: Brev): ByteArray {
        val uri = URI.create("$familieBrevUri/api/$sanityDataset/dokument/$målform/${brev.mal.apiNavn}/pdf")

        secureLogger.info("Kaller familie brev($uri) med data ${brev.data.toBrevString()}")
        return kallEksternTjeneste("famile-brev", uri, "Hente pdf for vedtaksbrev") {
            postForEntity(uri, brev.data)
        }
    }

    @Cacheable("begrunnelsestekster-for-nedtreksmeny", cacheManager = "shortCache")
    fun hentSanityBegrunnelser(): List<SanityBegrunnelse> {
        val uri = URI.create("$familieBrevUri/ba-sak/begrunnelser")

        val restSanityBegrunnelser =
            kallEksternTjeneste("famile-brev", uri, "Henter begrunnelser fra sanity via familie brev") {
                getForEntity<List<RestSanityBegrunnelse>>(uri)
            }

        return restSanityBegrunnelser.map { it.tilSanityBegrunnelse() }
    }

    @Cacheable("begrunnelsestekst", cacheManager = "shortCache")
    fun hentBegrunnelsestekst(begrunnelseData: BegrunnelseData): String {
        val uri = URI.create("$familieBrevUri/ba-sak/begrunnelser/${begrunnelseData.apiNavn}/tekst/")
        secureLogger.info("Kaller familie brev($uri) med data $begrunnelseData")

        return kallEksternTjeneste("famile-brev", uri, "Henter begrunnelsestekst") {
            postForEntity(uri, begrunnelseData)
        }
    }
}
