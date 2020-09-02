package no.nav.familie.ba.sak.vedtak.producer

import no.nav.familie.eksterne.kontrakter.VedtakDVH
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.UUID.randomUUID

@Service
@Profile("kafka-lokal", "preprod", "prod")
class VedtakKafkaProducer {

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, VedtakDVH>

    fun sendMessage(vedtak: VedtakDVH) {
        val response = kafkaTemplate.send(TOPIC, randomUUID().toString(), vedtak).get()
        logger.info("$TOPIC -> message sent -> ${response.recordMetadata.offset()}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(VedtakKafkaProducer::class.java)
        private const val TOPIC = "aapen-barnetrygd-vedtak-v1"
    }
}