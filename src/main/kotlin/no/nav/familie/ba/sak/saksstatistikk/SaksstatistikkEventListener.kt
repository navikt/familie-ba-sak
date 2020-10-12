package no.nav.familie.ba.sak.saksstatistikk

import no.nav.familie.ba.sak.vedtak.producer.KafkaProducer
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class SaksstatistikkEventListener(private val saksstatistikkService: SaksstatistikkService,
                                  private val kafkaProducer: KafkaProducer) : ApplicationListener<SaksstatistikkEvent> {

    override fun onApplicationEvent(event: SaksstatistikkEvent) {
        if (event.behandlingId != null) {
            saksstatistikkService.mapTilBehandlingDVH(event.behandlingId, event.forrigeBehandlingId).also {
                kafkaProducer.sendMessage(it)
            }
        } else if (event.fagsakId != null){
            saksstatistikkService.mapTilSakDvh(event.fagsakId).also {
                kafkaProducer.sendMessage(it)
            }
        }
    }
}