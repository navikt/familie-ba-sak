package no.nav.familie.ba.sak.hendelser

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

@Service
@Profile("!e2e && !dev")
class EFConsumer() {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @KafkaListener(topics = ["aapen-ef-overgangstonad-v1"],
                   containerFactory = "kafkaEFListenerContainerFactory")
    fun listen(cr: ConsumerRecord<String, String>, ack: Acknowledgment) {
        val melding = cr.value()

        logger.info("Melding fra EF. Key: ", cr.key())
        secureLogger.info("Melding fra EF. Key : " + cr.key() + ", Value: " + melding)

        ack.acknowledge()
    }
}