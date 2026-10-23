package no.nav.familie.ba.sak.ekstern.pensjon.kafka

import no.nav.familie.kontrakter.felles.jsonMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

interface PensjonKafkaProducer {
    fun sendMeldingOmFerdigstiltBehandlingForPensjon(ferdigstiltBehandlingForPensjonPayload: FerdigstiltBehandlingForPensjonPayload)
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
    override fun sendMeldingOmFerdigstiltBehandlingForPensjon(ferdigstiltBehandlingForPensjonPayload: FerdigstiltBehandlingForPensjonPayload) {
        val json = jsonMapper.writeValueAsString(ferdigstiltBehandlingForPensjonPayload)
        try {
            val future = kafkaTemplate.send(FERDIGSTILT_BEHANDLING_FOR_PENSJON_V1_TOPIC, ferdigstiltBehandlingForPensjonPayload.ident, json)
            future.get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS) // Vent på at meldingen er sendt
        } catch (e: Exception) {
            logger.error("Feil ved sending av melding om ferdigstilt behandling for pensjon", e)
            throw e
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DefaultPensjonKafkaProducer::class.java)
        private const val SEND_TIMEOUT_SECONDS = 5L
        const val FERDIGSTILT_BEHANDLING_FOR_PENSJON_V1_TOPIC = "teamfamilie.ferdigstilt-behandling-for-pensjon-v1"
    }
}

@Service
class MockPensjonKafkaProducer : PensjonKafkaProducer {
    private val logger = LoggerFactory.getLogger(MockPensjonKafkaProducer::class.java)

    override fun sendMeldingOmFerdigstiltBehandlingForPensjon(ferdigstiltBehandlingForPensjonPayload: FerdigstiltBehandlingForPensjonPayload) {
        logger.info("Sender ikke melding om ferdigstilt behandling for pensjon fordi Kafka-producer er skrudd av")
    }
}
