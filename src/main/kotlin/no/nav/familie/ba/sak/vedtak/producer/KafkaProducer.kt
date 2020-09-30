package no.nav.familie.ba.sak.vedtak.producer

import no.nav.familie.eksterne.kontrakter.VedtakDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

interface KafkaProducer {
    fun sendMessage(vedtak: VedtakDVH): Long
    fun sendMessage(behandling: BehandlingDVH): Long
}

@Service
@ConditionalOnProperty(
        value=["funksjonsbrytere.kafka.producer.enabled"],
        havingValue = "true",
        matchIfMissing = false)
@Primary
class DefaultKafkaProducer : KafkaProducer {

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    override fun sendMessage(vedtak: VedtakDVH): Long {
        val response = kafkaTemplate.send(VEDTAK_TOPIC, vedtak.behandlingsId, vedtak).get()
        logger.info("$VEDTAK_TOPIC -> message sent -> ${response.recordMetadata.offset()}")
        return response.recordMetadata.offset()
    }

    override fun sendMessage(behandling: BehandlingDVH): Long {
        val response = kafkaTemplate.send(SAKSSTATISTIKK_TOPIC, behandling.behandlingId, behandling).get()
        logger.info("$SAKSSTATISTIKK_TOPIC -> message sent -> ${response.recordMetadata.offset()}")
        return response.recordMetadata.offset()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DefaultKafkaProducer::class.java)
        private const val VEDTAK_TOPIC = "aapen-barnetrygd-vedtak-v1"
        private const val SAKSSTATISTIKK_TOPIC = "aapen-barnetrygd-saksstatistkk-v1"
    }
}

@Service
class MockKafkaProducer : KafkaProducer {


    val logger = LoggerFactory.getLogger(this::class.java)

    override fun sendMessage(vedtak: VedtakDVH): Long {
        logger.info("Skipper sending av vedtak for ${vedtak.behandlingsId} fordi kafka ikke er enablet")
        return 0
    }

    override fun sendMessage(behandling: BehandlingDVH): Long {
        logger.info("Skipper sending av saksstatistikk behandling for ${behandling.behandlingId} fordi kafka ikke er enablet")
        return 0
    }
}