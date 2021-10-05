package no.nav.familie.ba.sak.ekstern.tilbakekreving

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRespons
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import java.util.concurrent.CountDownLatch

@Service
@Profile("!e2e")
@ConditionalOnProperty(
    value = ["funksjonsbrytere.kafka.producer.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class HentFagsystemsbehandlingRequestConsumer(private val fagsystemsbehandlingService: FagsystemsbehandlingService) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    var latch: CountDownLatch = CountDownLatch(1)

    @KafkaListener(
        id = "familie-ba-sak",
        topics = ["teamfamilie.privat-tbk-hentfagsystemsbehandling-request-topic"],
        containerFactory = "concurrentKafkaListenerContainerFactory"
    )
    fun listen(consumerRecord: ConsumerRecord<String, String>, ack: Acknowledgment) {
        logger.info("HentFagsystemsbehandlingRequest er mottatt i kafka $consumerRecord")
        secureLogger.info("HentFagsystemsbehandlingRequest er mottatt i kafka $consumerRecord")

        val data: String = consumerRecord.value()
        val key: String = consumerRecord.key()
        val request: HentFagsystemsbehandlingRequest = objectMapper.readValue(data, HentFagsystemsbehandlingRequest::class.java)

        val fagsystemsbehandling = try {
            fagsystemsbehandlingService.hentFagsystemsbehandling(request)
        } catch (e: Exception) {
            logger.warn(
                "Noe gikk galt mens sender HentFagsystemsbehandlingRespons for behandling=${request.eksternId}. " +
                    "Feiler med ${e.message}"
            )
            HentFagsystemsbehandlingRespons(feilMelding = e.message)
        }
        fagsystemsbehandlingService.sendFagsystemsbehandling(fagsystemsbehandling, key, request.eksternId)
        latch.countDown()
        ack.acknowledge()
    }
}
