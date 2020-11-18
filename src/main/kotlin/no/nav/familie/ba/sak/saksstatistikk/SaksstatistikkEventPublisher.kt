package no.nav.familie.ba.sak.saksstatistikk

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component


@Component
class SaksstatistikkEventPublisher {

    @Autowired
    lateinit var applicationEventPublisher: ApplicationEventPublisher

    fun publiserBehandlingsstatistikk(behandlingId: Long, forrigeBehandlingId: Long?) {
        applicationEventPublisher.publishEvent(SaksstatistikkEvent(this, null, behandlingId, forrigeBehandlingId))
    }

    fun publiserSaksstatistikk(fagsakId: Long) {
        applicationEventPublisher.publishEvent(SaksstatistikkEvent(this, fagsakId, null, null))
    }
}

class SaksstatistikkEvent(source: Any,
                          val fagsakId: Long?,
                          val behandlingId: Long?,
                          val forrigeBehandlingId: Long?) : ApplicationEvent(source)
