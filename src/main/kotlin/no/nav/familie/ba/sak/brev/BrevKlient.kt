package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.brev.domene.maler.Brev
import no.nav.familie.ba.sak.common.assertGenerelleSuksessKriterier
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.log.NavHttpHeaders
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.net.URI

@Component
class BrevKlient(
        @Value("\${FAMILIE_BREV_API_URL}") private val familieBrevUri: String,
        private val restTemplate: RestTemplate
) {

    fun genererBrev(målform: String, malnavn: String, body: Brev): ByteArray {
        val uri = URI.create("$familieBrevUri/api/ba-brev/dokument/${målform}/${malnavn}/pdf")
        val request = RequestEntity.post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .acceptCharset(Charsets.UTF_8)
                .header(NavHttpHeaders.NAV_CALL_ID.asString(), MDC.get(MDCConstants.MDC_CALL_ID))
                .body(body)

        secureLogger.info("Kaller familie brev($uri) med data ${body.toBrevString()}")
        val response = restTemplate.exchange<Ressurs<ByteArray>>(request)
        assertGenerelleSuksessKriterier(response.body)
        return response.body?.data ?: error("Klarte ikke generere brev med familie-brev")
    }

    companion object {

        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
