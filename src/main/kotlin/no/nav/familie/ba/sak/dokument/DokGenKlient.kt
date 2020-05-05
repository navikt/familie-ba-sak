package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.restDomene.DocFormat
import no.nav.familie.ba.sak.behandling.restDomene.DocFormat.HTML
import no.nav.familie.ba.sak.behandling.restDomene.DocFormat.PDF
import no.nav.familie.ba.sak.dokument.domene.DokumentHeaderFelter
import no.nav.familie.ba.sak.dokument.domene.DokumentRequest
import no.nav.familie.ba.sak.dokument.domene.MalMedData
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.NavHttpHeaders
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.net.URI

@Service
class DokGenKlient(
        @Value("\${FAMILIE_BA_DOKGEN_API_URL}") private val dokgenServiceUri: String,
        private val restTemplate: RestTemplate
) {

    fun hentMarkdownForMal(malMedData: MalMedData): String {
        val url = URI.create("$dokgenServiceUri/template/${malMedData.mal}/create-markdown")
        LOG.info("hent markdown fra: $url")
        val response = utførRequest(lagPostRequest(url, malMedData.fletteFelter), String::class.java)
        return response.body.orEmpty()
    }

    fun lagHtmlFraMarkdown(template: String, markdown: String, dokumentHeaderFelter: DokumentHeaderFelter): String {
        val request = lagDokumentRequestForMarkdown(HTML, template, markdown, dokumentHeaderFelter)
        val response = utførRequest(request, String::class.java)
        return response.body.orEmpty()
    }

    fun lagPdfFraMarkdown(template: String, markdown: String, dokumentHeaderFelter: DokumentHeaderFelter): ByteArray {
        val request = lagDokumentRequestForMarkdown(PDF, template, markdown, dokumentHeaderFelter)
        val response = utførRequest(request, ByteArray::class.java)
        return response.body!!
    }

    fun lagDokumentRequestForMarkdown(format: DocFormat,
                                      template: String,
                                      markdown: String,
                                      dokumentHeaderFelter: DokumentHeaderFelter): RequestEntity<String> {
        val url = URI.create("$dokgenServiceUri/template/${template}/create-doc")
        val body = DokumentRequest(format,
                                   markdown,
                                   true,
                                   null,
                                   true,
                                   objectMapper.writeValueAsString(dokumentHeaderFelter))
        return lagPostRequest(url, objectMapper.writeValueAsString(body))
    }

    private fun lagPostRequest(url: URI, body: String): RequestEntity<String> {
        return RequestEntity.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .acceptCharset(Charsets.UTF_8)
                .header(NavHttpHeaders.NAV_CALL_ID.asString(), MDC.get(MDCConstants.MDC_CALL_ID))
                .body(body)
    }

    fun <T : Any> utførRequest(request: RequestEntity<String>, responseType: Class<T>): ResponseEntity<T> {
        try {
            return restTemplate.exchange(request, responseType)
        } catch (e: HttpClientErrorException.NotFound) {
            error("Ugyldig mal.")
        } catch (e: RestClientException) {
            error("Ukjent feil ved dokumentgenerering.")
        }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(this::class.java)
    }
}
