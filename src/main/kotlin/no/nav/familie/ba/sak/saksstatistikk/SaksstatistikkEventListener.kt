package no.nav.familie.ba.sak.saksstatistikk

import no.nav.familie.ba.sak.vedtak.producer.KafkaProducer
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class SaksstatistikkEventListener(private val saksstatistikkService: SaksstatistikkService,
                                  private val kafkaProducer: KafkaProducer) : ApplicationListener<SaksstatistikkEvent> {

    override fun onApplicationEvent(event: SaksstatistikkEvent) {
        saksstatistikkService.loggBehandlingStatus(event.behandlingId, event.forrigeBehandlingId).also {
            kafkaProducer.sendMessage(it)
        }
    }
}