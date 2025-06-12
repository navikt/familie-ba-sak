package no.nav.familie.ba.sak.kjerne.minside

import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.tms.microfrontend.MicrofrontendMessageBuilder
import no.nav.tms.microfrontend.Sensitivitet
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class MinsideAktiveringService(
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
        val callId = MDC.get(MDCConstants.MDC_CALL_ID) ?: IdUtils.generateId()
        logger.info("Aktiverer minside mikrofrontend for personIdent: $personIdent, callId: $callId")
        kafkaTemplate.send(TOPIC, callId, aktiveringsmelding)
    }

    fun deaktiver(personIdent: String) {
        val deaktiveringsmelding =
            MicrofrontendMessageBuilder
                .disable {
                    ident = personIdent
                    initiatedBy = INITIATED_BY
                    microfrontendId = MICROFRONTEND_ID
                }.text()
        val callId = MDC.get(MDCConstants.MDC_CALL_ID) ?: IdUtils.generateId()
        logger.info("Deaktiverer minside mikrofrontend for personIdent: $personIdent, callId: $callId")
        kafkaTemplate.send(TOPIC, deaktiveringsmelding)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MinsideAktiveringService::class.java)
        private const val MICROFRONTEND_ID = "familie-ba-mikrofrontend-minside"
        private const val INITIATED_BY = "teamfamilie"
        private const val TOPIC = "min-side.aapen-microfrontend-v1"
    }
}
