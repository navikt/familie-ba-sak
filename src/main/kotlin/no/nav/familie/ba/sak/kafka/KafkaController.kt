package no.nav.familie.ba.sak.kafka

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController @RequestMapping(value = ["/kafka"]) @ProtectedWithClaims(issuer = "azuread")
class KafkaController @Autowired internal constructor(private val producer: Producer) {

    @GetMapping(value = ["/publish"]) @Unprotected fun sendMessageToKafkaTopic() {
        producer.sendMessage("FOO")
    }
}