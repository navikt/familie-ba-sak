package no.nav.familie.ba.sak.saksstatistikk

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component


@Component
class SaksstatistikkEventPublisher {

    @Autowired
    lateinit var applicationEventPublisher: ApplicationEventPublisher

    fun publish(behandlingId: Long, forrigeBehandlingId: Long?) {
        val customSpringEvent = SaksstatistikkEvent(this, behandlingId, forrigeBehandlingId)
        applicationEventPublisher.publishEvent(customSpringEvent)
    }
}

class SaksstatistikkEvent(source: Any, val behandlingId: Long, val forrigeBehandlingId: Long?) : ApplicationEvent(source)
