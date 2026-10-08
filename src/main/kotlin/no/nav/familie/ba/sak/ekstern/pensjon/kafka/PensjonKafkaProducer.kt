package no.nav.familie.ba.sak.ekstern.pensjon.kafka

import no.nav.familie.kontrakter.felles.jsonMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

interface PensjonKafkaProducer {
    fun sendMeldingOmFerdigstiltBehandlingTilPensjon(ferdigstiltBehandlingHendelse: FerdigstiltBehandlingHendelse)
}

@Service
@ConditionalOnProperty(
    value = ["funksjonsbrytere.kafka.producer.enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
@Primary
class DefaultPensjonKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) : PensjonKafkaProducer {
    override fun sendMeldingOmFerdigstiltBehandlingTilPensjon(ferdigstiltBehandlingHendelse: FerdigstiltBehandlingHendelse) {
        val json = jsonMapper.writeValueAsString(ferdigstiltBehandlingHendelse)
        try {
            kafkaTemplate
                .send(IDENTER_MED_FERDIGSTILT_BEHANDLING_V1_TOPIC, ferdigstiltBehandlingHendelse.ident, json)
                .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (e: Exception) {
            logger.error("Feil ved sending av melding om ferdigstilt behandling til pensjon", e)
            throw e
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DefaultPensjonKafkaProducer::class.java)
        private const val SEND_TIMEOUT_SECONDS = 5L
        const val IDENTER_MED_FERDIGSTILT_BEHANDLING_V1_TOPIC = "teamfamilie.identer-med-ferdigstilt-behandling-v1"
    }
}

@Service
class MockPensjonKafkaProducer : PensjonKafkaProducer {
    private val logger = LoggerFactory.getLogger(MockPensjonKafkaProducer::class.java)

    override fun sendMeldingOmFerdigstiltBehandlingTilPensjon(ferdigstiltBehandlingHendelse: FerdigstiltBehandlingHendelse) {
        logger.info("Sender ikke melding om ferdigstilt behandling til pensjon fordi Kafka-producer er skrudd av")
    }
}
