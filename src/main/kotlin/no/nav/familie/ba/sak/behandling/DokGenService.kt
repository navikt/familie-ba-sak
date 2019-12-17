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
import java.nio.charset.Charset
import java.time.format.DateTimeFormatter

@Service
class DokGenService(
        @Value("\${FAMILIE_BA_DOKGEN_API_URL}") private val dokgenServiceUri: String,
        private val restTemplate: RestTemplate)
{
    fun hentStønadBrevMarkdown(behandlingVedtak: BehandlingVedtak): String {
        val fletteFelter = mapTilBrevfelter(behandlingVedtak)
        return hentMarkdownForMal("Innvilget", fletteFelter)
    }

    private fun mapTilBrevfelter(vedtak: BehandlingVedtak): String {
        val datoFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val brevfelter = "{\"belop\": %s,\n" + // TODO
                "\"startDato\": \"%s\",\n" +
                "\"begrunnelse\": \"%s\",\n" +
                "\"etterbetaling\": %s,\n" +
                "\"antallTimer\": %s,\n" +
                "\"stotteProsent\": %s,\n" +
                "\"enhet\": \"%s\",\n" +
                "\"saksbehandler\": \"%s\"}"

        return String.format(
                brevfelter, 123, vedtak.stønadFom.format(datoFormat), "begrunnelse", false, 1, 100, "enhet", vedtak.ansvarligSaksbehandler
        )
    }

    fun lagHtmlFraMarkdown(markdown: String): String {
        val url = URI.create(dokgenServiceUri + "/template/markdown/to-html")
        val response = utførRequest(HttpMethod.POST, MediaType.TEXT_MARKDOWN, url, markdown)

        return response.body.orEmpty()
    }

    fun hentMarkdownForMal(malNavn: String, fletteFelter: String): String {
        val url = URI.create(dokgenServiceUri + "/template/" + malNavn + "/create-markdown")
        val response = utførRequest(HttpMethod.POST, MediaType.APPLICATION_JSON, url, fletteFelter)

        return response.body.orEmpty()
    }

    fun utførRequest(httpMethod: HttpMethod, mediaType: MediaType, requestUrl: URI, requestBody: Any? = null): ResponseEntity<String> {
        val headers = HttpHeaders()
        headers.contentType = mediaType
        headers.acceptCharset= listOf(Charsets.UTF_8)
        headers.add(NavHttpHeaders.NAV_CALL_ID.asString(), MDC.get(MDCConstants.MDC_CALL_ID))

        return restTemplate.exchange(requestUrl, httpMethod, HttpEntity(requestBody, headers), String::class.java)
    }

}