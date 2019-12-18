package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtak
import no.nav.familie.log.NavHttpHeaders
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.net.URI

@Service
class DokGenService(
    @Value("\${FAMILIE_BA_DOKGEN_API_URL}") private val dokgenServiceUri: String,
    private val restTemplate: RestTemplate
) {

    private val månedMap = mapOf(1 to "januar", 2 to "februar", 3 to "mars", 4 to "april", 5 to "mai", 6 to "juni",
        7 to "juli", 8 to "august", 9 to "september", 10 to "oktober", 11 to "november", 12 to "desember")

    fun hentStønadBrevMarkdown(behandlingVedtak: BehandlingVedtak): String {
        val fletteFelter = mapTilBrevfelter(behandlingVedtak)
        return hentMarkdownForMal("Innvilget", fletteFelter)
    }

    private fun mapTilBrevfelter(vedtak: BehandlingVedtak): String {
        val brevfelter = "{\"belop\": %s,\n" + // TODO hent fra dokgen (/template/{templateName}/schema)
            "\"startDato\": \"%s\",\n" +
            "\"etterbetaling\": %s,\n" +
            "\"enhet\": \"%s\",\n" +
            "\"fodselsnummer\": %s,\n" +
            "\"fodselsdato\": \"%s\",\n" +
            "\"saksbehandler\": \"%s\"}"

        val startDato = vedtak.stønadFom.let { dato -> månedMap[dato.monthValue] + " " + dato.year }

        return String.format( // TODO Bytt ut hardkodede felter med faktiske verdier
            brevfelter, 123, startDato, false, "enhet", vedtak.behandling.fagsak.personIdent?.ident, "24.12.19", vedtak.ansvarligSaksbehandler
        )
    }

    fun lagHtmlFraMarkdown(markdown: String): String {
        val url = URI.create(dokgenServiceUri + "/template/markdown/to-html")
        val response = utførRequest(HttpMethod.POST, MediaType.TEXT_MARKDOWN, url, markdown)

        return response.body.orEmpty()
    }

    private fun hentMarkdownForMal(malNavn: String, fletteFelter: String): String {
        val url = URI.create(dokgenServiceUri + "/template/" + malNavn + "/create-markdown")
        val response = utførRequest(HttpMethod.POST, MediaType.APPLICATION_JSON, url, fletteFelter)

        return response.body.orEmpty()
    }

    protected fun utførRequest(httpMethod: HttpMethod, mediaType: MediaType, requestUrl: URI, requestBody: Any? = null): ResponseEntity<String> {
        val headers = HttpHeaders()
        headers.contentType = mediaType
        headers.acceptCharset = listOf(Charsets.UTF_8)
        headers.add(NavHttpHeaders.NAV_CALL_ID.asString(), MDC.get(MDCConstants.MDC_CALL_ID))

        return restTemplate.exchange(requestUrl, httpMethod, HttpEntity(requestBody, headers), String::class.java)
    }

}