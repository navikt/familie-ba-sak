package no.nav.familie.ba.sak.statistikk.saksstatistikk

import com.fasterxml.jackson.databind.JsonNode
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagring
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType
import no.nav.familie.kontrakter.felles.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Profile("!e2e")
class SaksstatistikkBehandlingConsumer(
    private val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository
) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @KafkaListener(
        topics = ["aapen-barnetrygd-saksstatistikk-behandling-v1"],
        id = BEHANDLING_CLIENT_ID,
        groupId = "familie-converter-behandling-2",
        containerFactory = "listenerContainerFactory",
        autoStartup = "false",
        clientIdPrefix = "behandling"
    )
    @Transactional
    fun consume(cr: ConsumerRecord<String, String>, ack: Acknowledgment) {
        logger.info("Leser fra behandling-topic med offset ${cr.offset()} " )
        val json = cr.value()

        val parent: JsonNode = objectMapper.readTree(json)
        val versjon: String = parent.path("versjon").asText()
        val funksjonellId: String = parent.path("funksjonellId").asText()
        val behandlingId: Long = parent.path("behandlingId").asLong()

        val mellomlagring = saksstatistikkMellomlagringRepository.findByFunksjonellIdAndKontraktVersjon(funksjonellId, versjon)
        if (mellomlagring == null) {
            saksstatistikkMellomlagringRepository.save(
                SaksstatistikkMellomlagring(
                    offsetVerdi = cr.offset(),
                    kontraktVersjon = versjon,
                    funksjonellId = funksjonellId,
                    json = json,
                    type = SaksstatistikkMellomlagringType.BEHANDLING,
                    typeId = behandlingId,
                    sendtTidspunkt = LocalDate.of(1970, 1, 1).atStartOfDay()
                )
            )
            logger.info("Mellomlagerer melding $funksjonellId " )
            secureLogger.info("Lagret melding $json")
        }
    }

    companion object {
        const val BEHANDLING_CLIENT_ID =  "saksstatistikk-behandling-mellomlagring"
    }
}