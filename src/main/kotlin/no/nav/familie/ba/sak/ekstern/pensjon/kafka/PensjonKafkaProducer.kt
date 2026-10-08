package no.nav.familie.ba.sak.ekstern.pensjon.kafka

import no.nav.familie.kontrakter.felles.jsonMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
@ConditionalOnProperty(
    value = ["funksjonsbrytere.kafka.producer.enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
@Primary
class PensjonKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    fun sendMeldingOmFerdigstiltBehandlingTilPensjon(ferdigstilBehandlingHendelse: FerdigstilBehandlingHendelse) {
        val json = jsonMapper.writeValueAsString(ferdigstilBehandlingHendelse)
        try {
            kafkaTemplate
                .send(AAPEN_IDENETER_MED_FERDIGSTILT_BEHANDLING_V1, ferdigstilBehandlingHendelse.ident, json)
                .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (e: Exception) {
            logger.error("Feil ved sending av melding om ferdigstilt behandling til pensjon", e)
            throw e
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PensjonKafkaProducer::class.java)
        private const val SEND_TIMEOUT_SECONDS = 5L
        val AAPEN_IDENETER_MED_FERDIGSTILT_BEHANDLING_V1 = "aapen-identer-med-ferdigstilt-behandling-v1"
    }
}
