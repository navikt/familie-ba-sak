package no.nav.familie.ba.sak.config

import io.mockk.*
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.ToTrinnKontrollService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import org.mockito.ArgumentMatchers.anyString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile


@TestConfiguration
class ToTrinnKontrollTestConfig {

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Bean
    @Profile("mock-totrinnkontroll")
    @Primary
    fun mockToTrinnKontrollService(): ToTrinnKontrollService {
        val toTrinnKontrollService: ToTrinnKontrollService = mockk()
        every { toTrinnKontrollService.valider2trinnVedIverksetting(any(), anyString()) } answers { //FIX ANY OF TYPE BEHANDLING
            val b = firstArg<Behandling>()
            behandlingService.oppdaterStatusPåBehandling(b.id, BehandlingStatus.GODKJENT)
        }
        return toTrinnKontrollService
    }
}