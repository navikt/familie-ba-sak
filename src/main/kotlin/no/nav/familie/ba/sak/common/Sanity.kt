package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.kjerne.brev.domene.RestSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.hentDokumenter
import no.nav.familie.kontrakter.felles.objectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

data class Respons(
    val ms: Int,
    val query: String,
    val result: List<RestSanityBegrunnelse>
)

fun hentSanityBegrunnelser(): List<SanityBegrunnelse> {
    val sanityUrl = "https://xsrv1mh6.apicdn.sanity.io/v2021-06-07/data/query/ba-brev"
    val query = hentDokumenter
    val parameters = java.net.URLEncoder.encode(query, "utf-8")

    val client = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create("$sanityUrl?query=$parameters"))
        .build()

    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    val json = objectMapper.readValue(
        response.body(),
        Respons::class.java
    )
    val restSanityBegrunnelser = json.result

    return restSanityBegrunnelser.map { it.tilSanityBegrunnelse() }
}
