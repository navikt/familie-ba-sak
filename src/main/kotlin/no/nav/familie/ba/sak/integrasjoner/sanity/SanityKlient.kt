package no.nav.familie.ba.sak.integrasjoner.sanity

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.kallEksternTjeneste
import no.nav.familie.ba.sak.integrasjoner.retryVedException
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelseDto
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.eøs.SanityEØSBegrunnelseDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity
import java.net.URI

const val SANITY_BASE_URL = "https://xsrv1mh6.api.sanity.io/v2021-06-07/data/query"

@Component
class SanityKlient(
    @Value("\${SANITY_DATASET}") private val datasett: String,
    private val restTemplate: RestTemplate,
    @Value("\${retry.backoff.delay:5000}") private val retryBackoffDelay: Long,
) {
    fun hentBegrunnelser(): List<SanityBegrunnelse> {
        val sanityUrl = "$SANITY_BASE_URL/$datasett"
        val hentBegrunnelserQuery = java.net.URLEncoder.encode(HENT_BEGRUNNELSER, "utf-8")

        val uri = URI.create("$sanityUrl?query=$hentBegrunnelserQuery")

        val restSanityBegrunnelser =
            kallEksternTjeneste(
                tjeneste = "Sanity",
                uri = uri,
                formål = "Henter begrunnelser fra sanity",
            ) {
                retryVedException(retryBackoffDelay).execute {
                    restTemplate.getForEntity<SanityBegrunnelserRespons>(uri).body?.result
                        ?: throw Feil("Klarer ikke å hente begrunnelser fra sanity")
                }
            }

        return restSanityBegrunnelser.mapNotNull { it.tilSanityBegrunnelse() }
    }

    fun hentEØSBegrunnelser(): List<SanityEØSBegrunnelse> {
        val sanityUrl = "$SANITY_BASE_URL/$datasett"
        val hentEØSBegrunnelserQuery = java.net.URLEncoder.encode(HENT_EØS_BEGRUNNELSER, "utf-8")

        val uri = URI.create("$sanityUrl?query=$hentEØSBegrunnelserQuery")

        return kallEksternTjeneste(
            tjeneste = "Sanity",
            uri = uri,
            formål = "Henter EØS-begrunnelser fra sanity",
        ) {
            retryVedException(retryBackoffDelay).execute {
                restTemplate
                    .getForEntity<SanityEØSBegrunnelserRespons>(uri)
                    .body
                    ?.result
                    ?.mapNotNull { it.tilSanityEØSBegrunnelse() }
                    ?: throw Feil("Klarer ikke å hente EØS-begrunnelser fra sanity")
            }
        }
    }
}

data class SanityBegrunnelserRespons(
    val ms: Int,
    val query: String,
    val result: List<SanityBegrunnelseDto>,
)

data class SanityEØSBegrunnelserRespons(
    val ms: Int,
    val query: String,
    val result: List<SanityEØSBegrunnelseDto>,
)
