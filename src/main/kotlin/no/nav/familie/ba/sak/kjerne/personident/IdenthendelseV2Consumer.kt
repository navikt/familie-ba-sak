package no.nav.familie.ba.sak.kjerne.personident

import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.log.mdc.MDCConstants
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Type
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@ConditionalOnProperty(
    value = ["funksjonsbrytere.kafka.producer.enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class IdenthendelseV2Consumer(
    val personidentService: PersonidentService,
) {
    @KafkaListener(
        id = "familie-ba-sak-aktorv2",
        groupId = "familie-ba-sak-aktorv2-group",
        topics = ["pdl.aktor-v2"],
        containerFactory = "kafkaAivenHendelseListenerAvroLatestContainerFactory",
    )
    @Transactional
    fun listen(
        consumerRecord: ConsumerRecord<String, Aktor?>,
        ack: Acknowledgment,
    ) {
        try {
//            Thread.sleep(60000) // Venter 1 min da det kan hende at PDL ikke er ferdig med å populere sine opplysninger rett etter at vi har lest meldingen
            MDC.put(MDCConstants.MDC_CALL_ID, UUID.randomUUID().toString())
            SECURE_LOGGER.info("Har mottatt ident-hendelse $consumerRecord")

            val aktør = consumerRecord.value()
            val aktørIdPåHendelse = consumerRecord.key()

            if (aktør == null) {
                log.warn("Tom aktør fra identhendelse")
                SECURE_LOGGER.warn("Tom aktør fra identhendelse med nøkkel $aktørIdPåHendelse")
            }

            val aktivAktørid =
                aktør
                    ?.identifikatorer
                    ?.singleOrNull { ident ->
                        ident.type == Type.AKTORID && ident.gjeldende
                    }?.idnummer
                    .toString()

            // I tilfeller som ved merge av hendelser vil man få både identhendelse på gammel og ny aktørid, så for å unngå duplikater så sender man bare på aktiv ident
            if (aktørIdPåHendelse.contains(aktivAktørid)) {
                aktør
                    ?.identifikatorer
                    ?.singleOrNull { ident ->
                        ident.type == Type.FOLKEREGISTERIDENT && ident.gjeldende
                    }?.also { folkeregisterident ->
                        personidentService.opprettTaskForIdentHendelse(PersonIdent(folkeregisterident.idnummer.toString()))
                    }
            } else {
                SECURE_LOGGER.info("Ignorerer å lage task på ident-hendelse fordi aktør $aktørIdPåHendelse ikke lenger er en gyldig aktør")
            }
        } catch (e: RuntimeException) {
            log.warn("Feil i prosessering av ident-hendelser", e)
            SECURE_LOGGER.warn("Feil i prosessering av ident-hendelser $consumerRecord", e)
//            throw RuntimeException("Feil i prosessering av ident-hendelser")
        } finally {
            MDC.clear()
        }
        ack.acknowledge()
    }

    companion object {
        val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLogger")
        val log: Logger = LoggerFactory.getLogger(IdenthendelseV2Consumer::class.java)
    }
}
