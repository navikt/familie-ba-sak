package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtak
import no.nav.familie.ba.sak.behandling.restDomene.DocFormat
import no.nav.familie.ba.sak.behandling.restDomene.DocFormat.*
import no.nav.familie.ba.sak.behandling.restDomene.DokumentRequest
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.NavHttpHeaders
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.net.URI

@Service
@Profile("!mock-dokgen-java")
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
            "\"fodselsnummer\": \"%s\",\n" +
            "\"fodselsdato\": \"%s\",\n" +
            "\"saksbehandler\": \"%s\"}"

        val startDato = "februar 2020" // TODO hent fra beregningen

        return String.format( // TODO Bytt ut hardkodede felter med faktiske verdier
            brevfelter, 123, startDato, false, "enhet", vedtak.behandling.fagsak.personIdent?.ident, "24.12.19", vedtak.ansvarligSaksbehandler
        )
    }

    private fun hentMarkdownForMal(malNavn: String, fletteFelter: String): String {
        val url = URI.create(dokgenServiceUri + "/template/" + malNavn + "/create-markdown")
        val response = restTemplate.exchange(lagPostRequest(url, fletteFelter), String::class.java)
        return response.body.orEmpty()
    }

    fun lagHtmlFraMarkdown(markdown: String): String {
        val request = lagDokumentRequestForMarkdown(HTML, markdown)
        val response = restTemplate.exchange(request, String::class.java)
        return response.body.orEmpty()
    }

    fun lagPdfFraMarkdown(markdown: String): ByteArray {
        val request = lagDokumentRequestForMarkdown(PDF, markdown)
        val response = restTemplate.exchange(request, ByteArray::class.java)
        return response.body!!
    }

    fun lagDokumentRequestForMarkdown(format: DocFormat, markdown: String): RequestEntity<String> {
        val url = URI.create(dokgenServiceUri + "/template/Innvilget/create-doc")
        val body = DokumentRequest(format, markdown, true, null, true, "{\"fodselsnummer\":\"12345678910\",\"navn\": \"navn\",\"adresse\": \"adresse\",\"postnr\": \"1626\",\"returadresse\": \"returadresse\",\"dokumentDato\": \"3. september 2019\"}")
        return lagPostRequest(url, body)
    }

    private fun lagPostRequest(url: URI, body: Any): RequestEntity<String> {
        return RequestEntity.post(url)
            .contentType(MediaType.APPLICATION_JSON)
            .acceptCharset(Charsets.UTF_8)
            .header(NavHttpHeaders.NAV_CALL_ID.asString(), MDC.get(MDCConstants.MDC_CALL_ID))
            .body(objectMapper.writeValueAsString(body))
    }

    protected fun utførRequest(httpMethod: HttpMethod, mediaType: MediaType, requestUrl: URI, requestBody: Any? = null): ResponseEntity<String> {
        val headers = HttpHeaders()
        headers.contentType = mediaType
        headers.acceptCharset = listOf(Charsets.UTF_8)
        headers.add(NavHttpHeaders.NAV_CALL_ID.asString(), MDC.get(MDCConstants.MDC_CALL_ID))

        return restTemplate.exchange(requestUrl, httpMethod, HttpEntity(requestBody, headers), String::class.java)
    }
}

@Service
@Profile("mock-dokgen-java")
class DokGenServiceMock: DokGenService(
        dokgenServiceUri = "dokgen_uri_mock",
        restTemplate = RestTemplate()
){
    override fun utførRequest(httpMethod: HttpMethod, mediaType: MediaType, requestUrl: URI, requestBody: Any?): ResponseEntity<String> {
        if(requestUrl.path.matches(Regex(".+create-markdown"))){
            return ResponseEntity.ok("# Vedtaksbrev Markdown (Mock)")
        }else if(requestUrl.path.matches(Regex(".+to-html"))){
            return ResponseEntity.ok("<HTML><H1>Vedtaksbrev HTML (Mock)</H1></HTML>")
        }

        return ResponseEntity.ok("")
    }
}