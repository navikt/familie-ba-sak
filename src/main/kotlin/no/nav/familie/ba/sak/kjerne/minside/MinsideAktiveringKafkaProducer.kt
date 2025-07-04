package no.nav.familie.ba.sak.kjerne.minside

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.kjerne.personident.Aktør
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
    fun aktiver(aktør: Aktør) {
        val personIdent = aktør.aktivFødselsnummer()
        val aktiveringsmelding =
            MicrofrontendMessageBuilder
                .enable {
                    ident = personIdent
                    initiatedBy = INITIATED_BY
                    microfrontendId = MICROFRONTEND_ID
                    sensitivitet = Sensitivitet.HIGH
                }.text()
        secureLogger.info("Aktiverer minside mikrofrontend for personIdent: $personIdent")
        sendMinsideMelding(message = aktiveringsmelding, key = aktør.aktørId)
    }

    fun deaktiver(aktør: Aktør) {
        val personIdent = aktør.aktivFødselsnummer()
        val deaktiveringsmelding =
            MicrofrontendMessageBuilder
                .disable {
                    ident = personIdent
                    initiatedBy = INITIATED_BY
                    microfrontendId = MICROFRONTEND_ID
                }.text()
        secureLogger.info("Deaktiverer minside mikrofrontend for personIdent: $personIdent")
        sendMinsideMelding(message = deaktiveringsmelding, key = aktør.aktørId)
    }

    private fun sendMinsideMelding(
        message: String,
        key: String,
    ) {
        try {
            // Venter på at meldingen er sendt før vi fortsetter
            kafkaTemplate.send(TOPIC, key, message).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (e: Exception) {
            logger.error("Feil ved sending av minside-melding til Kafka", e)
            throw e
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MinsideAktiveringKafkaProducer::class.java)
        private const val MICROFRONTEND_ID = "familie-ba-minside-mikrofrontend"
        private const val INITIATED_BY = "teamfamilie"
        private const val TOPIC = "min-side.aapen-microfrontend-v1"
        private const val SEND_TIMEOUT_SECONDS = 5L
    }
}
