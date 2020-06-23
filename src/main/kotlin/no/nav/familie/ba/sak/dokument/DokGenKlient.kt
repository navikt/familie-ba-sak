package no.nav.familie.ba.sak.dokument

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

    fun lagPdfForMal(malMedData: MalMedData, dokumentHeaderFelter: DokumentHeaderFelter): ByteArray {
        val url = URI.create("$dokgenServiceUri/template/${malMedData.mal}/create-doc")
        val request = lagPostRequest(url, DokumentRequest(docFormat = PDF,
                                                          templateContent = null,
                                                          precompiled = false,
                                                          mergeFields = malMedData.fletteFelter,
                                                          includeHeader = true,
                                                          headerFields = objectMapper.writeValueAsString(dokumentHeaderFelter)))
        val response = utførRequest(request ,ByteArray::class.java)
        return response.body!!
    }

    private fun lagPostRequest(url: URI, body: Any): RequestEntity<Any> {
        LOG.info("\"Gjør POST request mot $url")
        secureLogger.info("Gjør POST request mot $url med body ${objectMapper.writeValueAsString(body)}")
        return RequestEntity.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .acceptCharset(Charsets.UTF_8)
                .header(NavHttpHeaders.NAV_CALL_ID.asString(), MDC.get(MDCConstants.MDC_CALL_ID))
                .body(body)
    }

    fun <T : Any> utførRequest(request: RequestEntity<Any>, responseType: Class<T>): ResponseEntity<T> {
        try {
            return restTemplate.exchange(request, responseType)
        } catch (e: HttpClientErrorException.NotFound) {
            error("Ugyldig mal.")
        } catch (e: Exception) {
            if (request.url.host == "familie-ba-dokgen.adeo.no") {
                LOG.error("Feilet mot prod-gcp. Redirect'er request til prod-fss")
                val url_prod_fss = URI.create(request.url.toString().replace("adeo.no", "nais.adeo.no"))
                return utførRequest(lagPostRequest(url_prod_fss, request.body!!), responseType)
            }
            if (e is RestClientException) {
                error("Ukjent feil ved dokumentgenerering.")
            }
            throw e
        }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(this::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
