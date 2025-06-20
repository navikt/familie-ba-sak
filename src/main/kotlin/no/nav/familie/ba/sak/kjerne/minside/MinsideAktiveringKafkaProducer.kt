package no.nav.familie.ba.sak.kjerne.minside

import no.nav.tms.microfrontend.MicrofrontendMessageBuilder
import no.nav.tms.microfrontend.Sensitivitet
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class MinsideAktiveringKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    fun aktiver(personIdent: String) {
        val aktiveringsmelding =
            MicrofrontendMessageBuilder
                .enable {
                    ident = personIdent
                    initiatedBy = INITIATED_BY
                    microfrontendId = MICROFRONTEND_ID
                    sensitivitet = Sensitivitet.HIGH
                }.text()
        logger.info("Aktiverer minside mikrofrontend for personIdent: $personIdent")
        sendMinsideMelding(aktiveringsmelding)
    }

    fun deaktiver(personIdent: String) {
        val deaktiveringsmelding =
            MicrofrontendMessageBuilder
                .disable {
                    ident = personIdent
                    initiatedBy = INITIATED_BY
                    microfrontendId = MICROFRONTEND_ID
                }.text()
        logger.info("Deaktiverer minside mikrofrontend for personIdent: $personIdent")
        sendMinsideMelding(deaktiveringsmelding)
    }

    private fun sendMinsideMelding(message: String) {
        try {
            // Venter på at meldingen er sendt før vi fortsetter
            kafkaTemplate.send(TOPIC, message).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (e: Exception) {
            logger.error("Feil ved sending av minside-melding til Kafka", e)
            throw e
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MinsideAktiveringKafkaProducer::class.java)
        private const val MICROFRONTEND_ID = "familie-ba-mikrofrontend-minside"
        private const val INITIATED_BY = "team-baks"
        private const val TOPIC = "min-side.aapen-microfrontend-v1"
        private const val SEND_TIMEOUT_SECONDS = 5L
    }
}
