package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.brev.domene.maler.Brev
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import java.net.URI

@Component
class BrevKlient(
        @Value("\${FAMILIE_BREV_API_URL}") private val familieBrevUri: String,
        private val restTemplate: RestTemplate
) {

    fun genererBrev(målform: String, brev: Brev): ByteArray {
        val url = URI.create("$familieBrevUri/api/ba-brev/dokument/${målform}/${brev.type.apiNavn}/pdf")
        secureLogger.info("Kaller familie brev($url) med data ${brev.data.toBrevString()}")
        logger.info("Kaller familie brev($url) med data ${brev.data.toBrevString()}")
        val response = restTemplate.postForEntity<ByteArray>(url, brev.data)
        return response.body ?: error("Klarte ikke generere brev med familie-brev")
    }

    companion object {
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
        private val logger = LoggerFactory.getLogger(BrevKlient::class.java)
    }
}
