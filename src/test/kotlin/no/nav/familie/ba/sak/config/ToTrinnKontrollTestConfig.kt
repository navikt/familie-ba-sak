package no.nav.familie.ba.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.ToTrinnKontrollService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.vedtak.Beslutning
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
        every { toTrinnKontrollService.valider2trinnVedBeslutningOmIverksetting(any<Behandling>(), any<String>(), any<Beslutning>()) } answers {
            val behandling = firstArg<Behandling>()
            val beslutning = lastArg<Beslutning>()
            behandlingService.oppdaterStatusPåBehandling(behandling.id, if(beslutning.erGodkjent()) BehandlingStatus.GODKJENT else BehandlingStatus.UNDERKJENT_AV_BESLUTTER )
        }
        return toTrinnKontrollService
    }
}