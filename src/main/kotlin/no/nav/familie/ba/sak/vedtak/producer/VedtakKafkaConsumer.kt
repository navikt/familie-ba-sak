package no.nav.familie.ba.sak.vedtak.producer

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.io.IOException

/**
 * For midlertidig testbruk, skal fjernes etter testperiode.
 */
@Service
@Profile("preprod", "kafka-lokal")
class VedtakKafkaConsumer {
    private val logger = LoggerFactory.getLogger(VedtakKafkaProducer::class.java)

    @KafkaListener(topics = ["aapen-barnetrygd-vedtak-v1"], groupId = "group_id")
    fun consume(message: String?) {
        logger.info(String.format("aapen-barnetrygd-vedtak-v1 -> Consumed message -> %s", message))
    }
}