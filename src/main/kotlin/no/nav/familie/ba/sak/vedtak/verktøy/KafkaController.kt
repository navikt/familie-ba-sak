package no.nav.familie.ba.sak.vedtak.verktøy

import no.nav.familie.ba.sak.vedtak.producer.KafkaProducer
import no.nav.familie.eksterne.kontrakter.VedtakDVH
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/api/kafka"])
@ProtectedWithClaims(issuer = "azuread")
class KafkaController @Autowired internal constructor(private val producer: KafkaProducer) {

    /**
     * Sender vedtak til åpen kø aapen-barnetrygd-vedtak-v1.
     * For midlertidig testbruk og skal slettes.
     */
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Deprecated("For midlertidig testbruk")
    fun sendMessageToKafkaTopic(@RequestBody vedtak: VedtakDVH) {
        producer.sendMessage(vedtak)
    }
}