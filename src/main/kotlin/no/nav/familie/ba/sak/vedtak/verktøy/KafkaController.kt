package no.nav.familie.ba.sak.vedtak.verktøy

import no.nav.familie.ba.sak.vedtak.producer.VedtakKafkaProducer
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController @RequestMapping(value = ["/api/kafka"])
@ProtectedWithClaims(issuer = "azuread")
@Profile("kafka-lokal", "preprod", "prod")
class KafkaController @Autowired internal constructor(private val producer: VedtakKafkaProducer) {

    /**
     * Sender vedtak til åpen kø aapen-barnetrygd-vedtak-v1.
     * For midlertidig testbruk og skal slettes.
     */
    @PostMapping()
    @Deprecated("For midlertidig testbruk")
    fun sendMessageToKafkaTopic(@RequestBody vedtak: String) {
        producer.sendMessage(vedtak)
    }
}