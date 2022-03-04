package no.nav.familie.ba.sak.integrasjoner.sanity

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.restTemplate
import no.nav.familie.ba.sak.kjerne.brev.BrevKlient
import no.nav.familie.ba.sak.kjerne.brev.domene.RestSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import org.slf4j.LoggerFactory
import org.springframework.web.client.getForEntity
import java.net.URI

data class SanityRespons(
    val ms: Int,
    val query: String,
    val result: List<RestSanityBegrunnelse>
)

private val logger = LoggerFactory.getLogger(BrevKlient::class.java)

fun hentSanityBegrunnelser(datasett: String = "ba-brev"): List<SanityBegrunnelse> {
    val sanityUrl = "https://xsrv1mh6.apicdn.sanity.io/v2021-06-07/data/query/$datasett"
    val query = hentDokumenter
    val parameters = java.net.URLEncoder.encode(query, "utf-8")

    logger.info("Henter begrunnelser fra sanity")

    val response = restTemplate.getForEntity<SanityRespons>(URI.create("$sanityUrl?query=$parameters"))
    val restSanityBegrunnelser = response.body?.result
        ?: throw Feil("Klarer ikke å hente begrunnelser fra sanity")

    return restSanityBegrunnelser.map { it.tilSanityBegrunnelse() }
}
