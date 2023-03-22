package no.nav.familie.ba.sak.statistikk.saksstatistikk

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class SaksstatistikkEventPublisher {

    @Autowired
    lateinit var applicationEventPublisher: ApplicationEventPublisher

    fun publiserBehandlingsstatistikk(behandlingId: BehandlingId) {
        applicationEventPublisher.publishEvent(SaksstatistikkEvent(this, null, behandlingId.id))
    }

    fun publiserSaksstatistikk(fagsakId: Long) {
        applicationEventPublisher.publishEvent(SaksstatistikkEvent(this, fagsakId, null))
    }
}

class SaksstatistikkEvent(
    source: Any,
    val fagsakId: Long?,
    val behandlingId: Long?
) : ApplicationEvent(source)
