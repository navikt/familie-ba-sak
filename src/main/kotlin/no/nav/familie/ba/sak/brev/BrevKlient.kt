package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.brev.domene.maler.Brev
import no.nav.familie.log.NavHttpHeaders
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URI

@Component
class BrevKlient(
        @Value("\${FAMILIE_BREV_API_URL}") private val familieBrevUri: String,
        private val restTemplate: RestTemplate
) {
    fun genererBrev(målform: String, brev: Brev): ByteArray {
        val url = URI.create("$familieBrevUri/api/ba-brev/dokument/${målform}/${brev.brevType.apiNavn}/pdf")
        val request = RequestEntity.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .acceptCharset(Charsets.UTF_8)
                .header(NavHttpHeaders.NAV_CALL_ID.asString(), MDC.get(MDCConstants.MDC_CALL_ID))
                .body(brev.brevData)

        secureLogger.info("Kaller familie brev($url) med data ${brev.brevData.toBrevString()}")
        val response = restTemplate.exchange(request, ByteArray::class.java)
        return response.body ?: error("Klarte ikke generere brev med familie-brev")
    }

    companion object {
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
