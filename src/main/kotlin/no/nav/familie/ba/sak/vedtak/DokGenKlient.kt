package no.nav.familie.ba.sak.vedtak

import no.nav.familie.log.NavHttpHeaders
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.http.HttpMethod.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.net.URI

@Service
class DokGenKlient @Autowired constructor(
    @Value("\${FAMILIE_BA_DOKGEN_API_URL}") private val dokgenServiceUri: String,
    private val restTemplateMedProxy: RestTemplate) {

    fun lagHtmlFraMarkdown(markdown: String): String {
        val url = URI.create(dokgenServiceUri + "/template/Innvilget/preview-html/Innvilget1") // Kan laste opp og erstatte "Innvilget" med "Fritekstmal" f.eks
        val response = utførRequest(POST, String::class.java, url, markdown)

        if (!response.statusCode.is2xxSuccessful) {
            throw RuntimeException(response.toString()) //TODO feilhåndtering
        }
        return response.body.orEmpty()
    }

    fun hentMarkdownForMal(malNavn: String) : String {
        val url = URI.create(dokgenServiceUri + "/template/" + malNavn + "/markdown")
        val response = utførRequest(GET, String::class.java, url)

        if (!response.statusCode.is2xxSuccessful) {
            throw RuntimeException(response.toString()) //TODO feilhåndtering
        }
        return response.body.orEmpty()
    }

    fun <T> utførRequest(httpMethod: HttpMethod, responseType: Class<T>, requestUrl: URI, requestBody: Any? = null): ResponseEntity<T> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.TEXT_PLAIN
        headers.add(NavHttpHeaders.NAV_CALL_ID.asString(), MDC.get(MDCConstants.MDC_CALL_ID))
        return restTemplateMedProxy.exchange(requestUrl, httpMethod, HttpEntity(requestBody,headers), responseType)
    }

}