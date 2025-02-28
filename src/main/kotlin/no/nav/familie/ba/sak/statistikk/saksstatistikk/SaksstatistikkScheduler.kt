package no.nav.familie.ba.sak.statistikk.saksstatistikk

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.LeaderClientService
import no.nav.familie.ba.sak.statistikk.producer.KafkaProducer
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SaksstatistikkScheduler(
    val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository,
    val kafkaProducer: KafkaProducer,
    val leaderClientService: LeaderClientService,
) : ApplicationListener<ContextClosedEvent> {
    @Volatile private var isShuttingDown = false

    @Scheduled(fixedDelay = 60000)
    fun sendKafkameldinger() {
        if (!isShuttingDown && leaderClientService.isLeader()) {
            sendSaksstatistikk()
        }
    }

    fun sendSaksstatistikk() {
        val meldinger = saksstatistikkMellomlagringRepository.finnMeldingerKlarForSending()

        for (melding in meldinger) {
            try {
                when (melding.type) {
                    SaksstatistikkMellomlagringType.SAK -> {
                        kafkaProducer.sendMessageForTopicSak(melding)
                    }

                    SaksstatistikkMellomlagringType.BEHANDLING -> {
                        kafkaProducer.sendMessageForTopicBehandling(melding)
                    }
                }
            } catch (e: Exception) {
                logger.error("Kunne ikke sende melding med ${melding.id},type ${melding.type} og fagsakId/behandlingid=${melding.typeId} til kafka")
                secureLogger.error("Kunne ikke sende melding med ${melding.id},type ${melding.type} og fagsakId/behandlingid=${melding.typeId} til kafka. $melding", e)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SaksstatistikkScheduler::class.java)
    }

    override fun onApplicationEvent(event: ContextClosedEvent) {
        isShuttingDown = true
    }
}
