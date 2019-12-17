package no.nav.familie.ba.sak.behandling

import no.nav.familie.log.NavHttpHeaders
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.MDC
import org.springframework.http.*
import org.springframework.http.HttpMethod.POST
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_MARKDOWN
import org.springframework.web.client.RestTemplate
import java.net.URI

class DokGenKlient(
    private val dokgenServiceUri: String,
    private val restTemplate: RestTemplate
) {

    fun lagHtmlFraMarkdown(markdown: String): String {
        val url = URI.create(dokgenServiceUri + "/template/markdown/to-html")
        val response = utførRequest(POST, TEXT_MARKDOWN, url, markdown)

        if (!response.statusCode.is2xxSuccessful) {
            throw RuntimeException(response.toString()) //TODO feilhåndtering
        }
        return response.body.orEmpty()
    }

    fun hentMarkdownForMal(malNavn: String, fletteFelter: String): String {
        val url = URI.create(dokgenServiceUri + "/template/" + malNavn + "/create-markdown")
        val response = utførRequest(POST, APPLICATION_JSON, url, fletteFelter)

        if (!response.statusCode.is2xxSuccessful) {
            throw RuntimeException(response.toString()) //TODO feilhåndtering
        }
        return response.body.orEmpty()
    }

    fun utførRequest(httpMethod: HttpMethod, mediaType: MediaType, requestUrl: URI, requestBody: Any? = null): ResponseEntity<String> {
        val headers = HttpHeaders()
        headers.contentType = mediaType
        headers.add(NavHttpHeaders.NAV_CALL_ID.asString(), MDC.get(MDCConstants.MDC_CALL_ID))

        return restTemplate.exchange(requestUrl, httpMethod, HttpEntity(requestBody, headers), String::class.java)
    }

}