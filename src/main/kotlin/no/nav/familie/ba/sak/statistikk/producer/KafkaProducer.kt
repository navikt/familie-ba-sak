package no.nav.familie.ba.sak.statistikk.producer

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagring
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.eksterne.kontrakter.VedtakDVH
import no.nav.familie.eksterne.kontrakter.VedtakDVHV2
import no.nav.familie.eksterne.kontrakter.bisys.BarnetrygdBisysMelding
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRespons
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

interface KafkaProducer {

    fun sendMessageForTopicVedtak(vedtak: VedtakDVH): Long
    fun sendMessageForTopicVedtakV2(vedtakV2: VedtakDVHV2): Long
    fun sendMessageForTopicBehandling(melding: SaksstatistikkMellomlagring): Long
    fun sendMessageForTopicSak(melding: SaksstatistikkMellomlagring): Long

    fun sendFagsystemsbehandlingResponsForTopicTilbakekreving(
        melding: HentFagsystemsbehandlingRespons,
        key: String,
        behandlingId: String
    )

    fun sendBarnetrygdBisysMelding(
        behandlingId: String,
        barnetrygdBisysMelding: BarnetrygdBisysMelding
    )
}

@Service
@ConditionalOnProperty(
    value = ["funksjonsbrytere.kafka.producer.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
@Primary
@Profile("!preprod-gcp & !prod-gcp")
class DefaultKafkaProducer(val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository) :
    KafkaProducer {

    private val vedtakCounter = Metrics.counter(COUNTER_NAME, "type", "vedtak")
    private val vedtakV2Counter = Metrics.counter(COUNTER_NAME, "type", "vedtakV2")
    private val saksstatistikkSakDvhCounter = Metrics.counter(COUNTER_NAME, "type", "sak")
    private val saksstatistikkBehandlingDvhCounter = Metrics.counter(COUNTER_NAME, "type", "behandling")
    @Autowired
    @Qualifier("kafkaObjectMapper")
    lateinit var kafkaObjectMapper: ObjectMapper
    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Autowired
    lateinit var kafkaAivenTemplate: KafkaTemplate<String, String>

    override fun sendMessageForTopicVedtak(vedtak: VedtakDVH): Long {
        val response = kafkaTemplate.send(VEDTAK_TOPIC, vedtak.funksjonellId!!, vedtak).get()
        logger.info("$VEDTAK_TOPIC -> message sent -> ${response.recordMetadata.offset()}")
        vedtakCounter.increment()
        return response.recordMetadata.offset()
    }

    override fun sendMessageForTopicVedtakV2(vedtakV2: VedtakDVHV2): Long {
        val vedtakForDVHV2Melding =
            kafkaObjectMapper.writeValueAsString(vedtakV2)
        val response = kafkaAivenTemplate.send(VEDTAKV2_TOPIC, vedtakV2.funksjonellId!!, vedtakForDVHV2Melding).get()
        logger.info("$VEDTAKV2_TOPIC -> message sent -> ${response.recordMetadata.offset()}")
        vedtakV2Counter.increment()
        return response.recordMetadata.offset()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun sendMessageForTopicBehandling(melding: SaksstatistikkMellomlagring): Long {
        val response =
            kafkaTemplate.send(SAKSSTATISTIKK_BEHANDLING_TOPIC, melding.funksjonellId, melding.jsonToBehandlingDVH())
                .get()
        logger.info("$SAKSSTATISTIKK_BEHANDLING_TOPIC -> message sent -> ${response.recordMetadata.offset()}")
        saksstatistikkBehandlingDvhCounter.increment()
        melding.offsetVerdi = response.recordMetadata.offset()
        melding.sendtTidspunkt = LocalDateTime.now()
        saksstatistikkMellomlagringRepository.save(melding)
        return response.recordMetadata.offset()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun sendMessageForTopicSak(melding: SaksstatistikkMellomlagring): Long {
        val response = kafkaTemplate.send(SAKSSTATISTIKK_SAK_TOPIC, melding.funksjonellId, melding.jsonToSakDVH()).get()
        logger.info("$SAKSSTATISTIKK_SAK_TOPIC -> message sent -> ${response.recordMetadata.offset()}")
        saksstatistikkSakDvhCounter.increment()
        melding.offsetVerdi = response.recordMetadata.offset()
        melding.sendtTidspunkt = LocalDateTime.now()
        saksstatistikkMellomlagringRepository.save(melding)
        return response.recordMetadata.offset()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun sendFagsystemsbehandlingResponsForTopicTilbakekreving(
        melding: HentFagsystemsbehandlingRespons,
        key: String,
        behandlingId: String
    ) {
        val meldingIString: String = objectMapper.writeValueAsString(melding)

        kafkaAivenTemplate.send(FAGSYSTEMSBEHANDLING_RESPONS_TBK_TOPIC, key, meldingIString)
            .addCallback(
                {
                    logger.info(
                        "Melding på topic $FAGSYSTEMSBEHANDLING_RESPONS_TBK_TOPIC for " +
                            "$behandlingId med $key er sendt. " +
                            "Fikk offset ${it?.recordMetadata?.offset()}"
                    )
                },
                {
                    val feilmelding =
                        "Melding på topic $FAGSYSTEMSBEHANDLING_RESPONS_TBK_TOPIC kan ikke sendes for " +
                            "$behandlingId med $key. Feiler med ${it.message}"
                    logger.warn(feilmelding)
                    throw Feil(message = feilmelding)
                }
            )
    }

    override fun sendBarnetrygdBisysMelding(
        behandlingId: String,
        barnetrygdBisysMelding: BarnetrygdBisysMelding
    ) {
        val opphørBarnetrygdBisysMelding =
            objectMapper.writeValueAsString(barnetrygdBisysMelding)

        kafkaAivenTemplate.send(OPPHOER_BARNETRYGD_BISYS_TOPIC, behandlingId, opphørBarnetrygdBisysMelding)
            .addCallback(
                {
                    logger.info(
                        "Melding på topic $OPPHOER_BARNETRYGD_BISYS_TOPIC for " +
                            "$behandlingId er sendt. " +
                            "Fikk offset ${it?.recordMetadata?.offset()}"
                    )
                    secureLogger.info("Send barnetrygd bisys melding $opphørBarnetrygdBisysMelding")
                },
                {
                    val feilmelding =
                        "Melding på topic $OPPHOER_BARNETRYGD_BISYS_TOPIC kan ikke sendes for " +
                            "$behandlingId. Feiler med ${it.message}"
                    logger.warn(feilmelding)
                    throw Feil(message = feilmelding)
                }
            )
    }

    companion object {

        private val logger = LoggerFactory.getLogger(DefaultKafkaProducer::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
        private const val VEDTAK_TOPIC = "aapen-barnetrygd-vedtak-v1"
        private const val VEDTAKV2_TOPIC = "teamfamilie.aapen-barnetrygd-vedtak-v2"
        private const val SAKSSTATISTIKK_BEHANDLING_TOPIC = "aapen-barnetrygd-saksstatistikk-behandling-v1"
        private const val SAKSSTATISTIKK_SAK_TOPIC = "aapen-barnetrygd-saksstatistikk-sak-v1"
        private const val COUNTER_NAME = "familie.ba.sak.kafka.produsert"
        private const val FAGSYSTEMSBEHANDLING_RESPONS_TBK_TOPIC =
            "teamfamilie.privat-tbk-hentfagsystemsbehandling-respons-topic"
        const val OPPHOER_BARNETRYGD_BISYS_TOPIC = "teamfamilie.aapen-familie-ba-sak-opphoer-barnetrygd"
    }
}

@Service
class MockKafkaProducer(val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository) :
    KafkaProducer {

    override fun sendMessageForTopicVedtak(vedtak: VedtakDVH): Long {
        logger.info("Skipper sending av vedtak for ${vedtak.behandlingsId} fordi kafka ikke er enablet")

        sendteMeldinger["vedtak-${vedtak.behandlingsId}"] = vedtak
        return 0
    }

    override fun sendMessageForTopicVedtakV2(vedtak: VedtakDVHV2): Long {
        logger.info("Skipper sending av vedtakV2 for ${vedtak.behandlingsId} fordi kafka Aiven for DVH V2 ikke er enablet")

        sendteMeldinger["vedtakV2-${vedtak.behandlingsId}"] = vedtak
        return 0
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun sendMessageForTopicBehandling(melding: SaksstatistikkMellomlagring): Long {
        logger.info("Skipper sending av saksstatistikk behandling for ${melding.jsonToBehandlingDVH().behandlingId} fordi kafka ikke er enablet")
        sendteMeldinger["behandling-${melding.jsonToBehandlingDVH().behandlingId}"] = melding.jsonToBehandlingDVH()
        melding.offsetVerdi = 42
        melding.sendtTidspunkt = LocalDateTime.now()
        saksstatistikkMellomlagringRepository.save(melding)
        return 42
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun sendMessageForTopicSak(melding: SaksstatistikkMellomlagring): Long {
        logger.info("Skipper sending av saksstatistikk sak for ${melding.jsonToSakDVH().sakId} fordi kafka ikke er enablet")
        sendteMeldinger["sak-${melding.jsonToSakDVH().sakId}"] = melding.jsonToSakDVH()
        melding.offsetVerdi = 43
        melding.sendtTidspunkt = LocalDateTime.now()
        saksstatistikkMellomlagringRepository.save(melding)
        return 43
    }

    override fun sendFagsystemsbehandlingResponsForTopicTilbakekreving(
        melding: HentFagsystemsbehandlingRespons,
        key: String,
        behandlingId: String
    ) {
        logger.info("Skipper sending av fagsystemsbehandling respons for $behandlingId fordi kafka ikke er enablet")
    }

    override fun sendBarnetrygdBisysMelding(
        behandlingId: String,
        barnetrygdBisysMelding: BarnetrygdBisysMelding
    ) {
        logger.info("Skipper sending av sendOpphørBarnetrygdBisys respons for $behandlingId fordi kafka ikke er enablet")
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MockKafkaProducer::class.java)

        var sendteMeldinger = mutableMapOf<String, Any>()
    }
}
