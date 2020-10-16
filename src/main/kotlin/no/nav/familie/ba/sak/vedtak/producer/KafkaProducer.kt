package no.nav.familie.ba.sak.vedtak.producer

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.eksterne.kontrakter.VedtakDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

interface KafkaProducer {
    fun sendMessageForTopicVedtak(vedtak: VedtakDVH): Long
    fun sendMessageForTopicBehandling(behandling: BehandlingDVH): Long
    fun sendMessageForTopicSak(sak: SakDVH): Long
}

@Service
@ConditionalOnProperty(
        value=["funksjonsbrytere.kafka.producer.enabled"],
        havingValue = "true",
        matchIfMissing = false)
@Primary
class DefaultKafkaProducer : KafkaProducer {

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    override fun sendMessageForTopicVedtak(vedtak: VedtakDVH): Long {
        val response = kafkaTemplate.send(VEDTAK_TOPIC, vedtak.behandlingsId, vedtak).get()
        logger.info("$VEDTAK_TOPIC -> message sent -> ${response.recordMetadata.offset()}")
        return response.recordMetadata.offset()
    }

    override fun sendMessageForTopicBehandling(behandling: BehandlingDVH): Long {
        val response = kafkaTemplate.send(SAKSSTATISTIKK_BEHANDLING_TOPIC, behandling.behandlingId, behandling).get()
        logger.info("$SAKSSTATISTIKK_BEHANDLING_TOPIC -> message sent -> ${response.recordMetadata.offset()}")
        return response.recordMetadata.offset()
    }

    override fun sendMessageForTopicSak(sak: SakDVH): Long {
        val response = kafkaTemplate.send(SAKSSTATISTIKK_SAK_TOPIC, sak.sakId, sak).get()
        logger.info("$SAKSSTATISTIKK_SAK_TOPIC -> message sent -> ${response.recordMetadata.offset()}")
        return response.recordMetadata.offset()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DefaultKafkaProducer::class.java)
        private const val VEDTAK_TOPIC = "aapen-barnetrygd-vedtak-v1"
        private const val SAKSSTATISTIKK_BEHANDLING_TOPIC = "aapen-barnetrygd-saksstatistikk-behandling-v1"
        private const val SAKSSTATISTIKK_SAK_TOPIC = "aapen-barnetrygd-saksstatistikk-sak-v1"
    }
}

@Service
class MockKafkaProducer : KafkaProducer {

    val logger = LoggerFactory.getLogger(this::class.java)

    override fun sendMessageForTopicVedtak(vedtak: VedtakDVH): Long {
        logger.info("Skipper sending av vedtak for ${vedtak.behandlingsId} fordi kafka ikke er enablet")

        sendteMeldinger["vedtak-${vedtak.behandlingsId}"] = vedtak
        return 0
    }

    override fun sendMessageForTopicBehandling(behandling: BehandlingDVH): Long {
        logger.info("Skipper sending av saksstatistikk behandling for ${behandling.behandlingId} fordi kafka ikke er enablet")
        sendteMeldinger["behandling-${behandling.behandlingId}"] = behandling
        return 0
    }

    override fun sendMessageForTopicSak(sak: SakDVH): Long {
        logger.info("Skipper sending av saksstatistikk sak for ${sak.sakId} fordi kafka ikke er enablet")
        sendteMeldinger["sak-${sak.sakId}"] = sak
        return 0
    }

    companion object {
        var sendteMeldinger = mutableMapOf<String, Any>()

        fun meldingSendtFor(hendelse: Any): Any? {
            return when (hendelse) {
                is Behandling -> sendteMeldinger.get("behandling-${hendelse.id}")
                is Fagsak -> sendteMeldinger.get("sak-${hendelse.id}")
                is Vedtak -> sendteMeldinger.get("vedtak-${hendelse.behandling.id}")
                else -> throw NotImplementedError()
            }
        }
    }
}