package no.nav.familie.ba.sak.vedtak.producer

import no.nav.familie.eksterne.kontrakter.VedtakDVH
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.UUID.randomUUID

interface VedtakProducer {
    fun sendMessage(vedtak: VedtakDVH): Long
}

@Service
@ConditionalOnProperty(
        value=["funksjonsbrytere.vedtak.producer.enabled"],
        havingValue = "true",
        matchIfMissing = false)
@Primary
class VedtakKafkaProducer : VedtakProducer {

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, VedtakDVH>

    override fun sendMessage(vedtak: VedtakDVH): Long {
        val response = kafkaTemplate.send(TOPIC, vedtak.behandlingsId, vedtak).get()
        logger.info("$TOPIC -> message sent -> ${response.recordMetadata.offset()}")
        return response.recordMetadata.offset()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(VedtakKafkaProducer::class.java)
        private const val TOPIC = "aapen-barnetrygd-vedtak-v1"
    }
}

@Service
class VedtakMockProducer : VedtakProducer {


    val logger = LoggerFactory.getLogger(this::class.java)

    override fun sendMessage(vedtak: VedtakDVH): Long {
        logger.info("Skipper sending av vedtak for ${vedtak.behandlingsId} fordi kafka ikke er enablet")
        return 0
    }
}