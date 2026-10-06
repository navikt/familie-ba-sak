package no.nav.familie.ba.sak.ekstern.pensjon.kafka

import no.nav.familie.kontrakter.felles.jsonMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

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
        kafkaTemplate.send(AAPEN_IDENETER_MED_FERDIGSTILT_BEHANDLING_V1, ferdigstilBehandlingHendelse.ident, json)
    }

    companion object {
        val AAPEN_IDENETER_MED_FERDIGSTILT_BEHANDLING_V1 = "aapen-identer-med-ferdigstilt-behandling-v1"
    }
}
