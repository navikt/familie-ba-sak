package no.nav.familie.ba.sak.integrasjoner.sanity

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.kallEksternTjeneste
import no.nav.familie.ba.sak.config.restTemplate
import no.nav.familie.ba.sak.kjerne.brev.domene.RestSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.SanityEØSBegrunnelse
import org.springframework.web.client.getForEntity
import java.net.URI

data class SanityNasjonaleBegrunnelserRespons(
    val ms: Int,
    val query: String,
    val result: List<RestSanityBegrunnelse>
)

const val sanityBaseUrl = "https://xsrv1mh6.apicdn.sanity.io/v2021-06-07/data/query"

fun hentBegrunnelser(datasett: String = "ba-brev"): List<SanityBegrunnelse> {
    val sanityUrl = "$sanityBaseUrl/$datasett"
    val hentNasjonaleBegrunnelserQuery = java.net.URLEncoder.encode(hentNasjonaleBegrunnelser, "utf-8")

    val uri = URI.create("$sanityUrl?query=$hentNasjonaleBegrunnelserQuery")

    val restSanityBegrunnelser =
        kallEksternTjeneste(
            tjeneste = "Sanity",
            uri = uri,
            formål = "Henter nasjonale begrunnelser fra sanity"
        ) {
            restTemplate.getForEntity<SanityNasjonaleBegrunnelserRespons>(uri).body?.result
                ?: throw Feil("Klarer ikke å hente begrunnelser fra sanity")
        }

    return restSanityBegrunnelser.map { it.tilSanityBegrunnelse() }
}

data class SanityEØSBegrunnelserRespons(
    val ms: Int,
    val query: String,
    val result: List<SanityEØSBegrunnelse>
)

fun hentEØSBegrunnelser(datasett: String = "ba-brev"): List<SanityEØSBegrunnelse> {
    val sanityUrl = "https://xsrv1mh6.apicdn.sanity.io/v2021-06-07/data/query/$datasett"
    val hentEØSBegrunnelserQuery = java.net.URLEncoder.encode(hentEØSBegrunnelser, "utf-8")

    val uri = URI.create("$sanityUrl?query=$hentEØSBegrunnelserQuery")

    return kallEksternTjeneste(
        tjeneste = "Sanity",
        uri = uri,
        formål = "Henter EØS-begrunnelser fra sanity"
    ) {
        restTemplate.getForEntity<SanityEØSBegrunnelserRespons>(uri).body?.result
            ?: throw Feil("Klarer ikke å hente begrunnelser fra sanity")
    }
}
