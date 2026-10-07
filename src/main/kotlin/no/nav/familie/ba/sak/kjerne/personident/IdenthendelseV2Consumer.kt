package no.nav.familie.ba.sak.kjerne.personident

import no.nav.familie.ba.sak.common.secureLogger
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
import java.time.Duration
import java.util.UUID

@Service
@ConditionalOnProperty(
    value = ["funksjonsbrytere.kafka.producer.enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class IdenthendelseV2Consumer(
    val personidentService: PersonidentService,
    private val ventetidFørBehandling: Duration = Duration.ofMinutes(1),
) {
    @KafkaListener(
        id = "familie-ba-sak-aktorv2",
        groupId = "familie-ba-sak-aktorv2-group",
        topics = ["pdl.aktor-v2"],
        containerFactory = "kafkaAivenHendelseListenerAvroLatestContainerFactory",
    )
    fun listen(
        consumerRecords: List<ConsumerRecord<String, Aktor?>>,
        ack: Acknowledgment,
    ) {
        // Venter 1 min per batch da det kan hende at PDL ikke er ferdig med å populere sine opplysninger rett etter at vi har lest meldingen
        Thread.sleep(ventetidFørBehandling.toMillis())

        consumerRecords.forEachIndexed { indeks, consumerRecord ->
            try {
                behandleHendelse(consumerRecord)
            } catch (e: RuntimeException) {
                if (indeks > 0) ack.acknowledge(indeks - 1)
                log.warn("Feil i prosessering av ident-hendelser", e)
                secureLogger.warn("Feil i prosessering av ident-hendelser $consumerRecord", e)
                throw RuntimeException("Feil i prosessering av ident-hendelser")
            }
        }
        ack.acknowledge()
    }

    private fun behandleHendelse(consumerRecord: ConsumerRecord<String, Aktor?>) {
        try {
            MDC.put(MDCConstants.MDC_CALL_ID, UUID.randomUUID().toString())
            secureLogger.info("Har mottatt ident-hendelse $consumerRecord")

            val aktør = consumerRecord.value()
            val aktørIdPåHendelse = consumerRecord.key()

            if (aktør == null) {
                log.warn("Tom aktør fra identhendelse")
                secureLogger.warn("Tom aktør fra identhendelse med nøkkel $aktørIdPåHendelse")
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
                secureLogger.info("Ignorerer å lage task på ident-hendelse fordi aktør $aktørIdPåHendelse ikke lenger er en gyldig aktør")
            }
        } finally {
            MDC.clear()
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(IdenthendelseV2Consumer::class.java)
    }
}
