package no.nav.familie.ba.sak.saksstatistikk

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class SaksstatistikkEventPublisherFailSafe: SaksstatistikkEventPublisher() {

    @Bean
    @Primary
    fun safeSaksstatistikkEventPublisher(): SaksstatistikkEventPublisher {
        return this
    }

    override fun publish(behandlingId: Long, forrigeBehandlingId: Long?) {
        runCatching {
            super.publish(behandlingId, forrigeBehandlingId)
        }
    }

    override fun publish(fagsakId: Long) {
        runCatching {
            super.publish(fagsakId)
        }
    }
}