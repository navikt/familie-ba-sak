package no.nav.familie.ba.sak.kjerne.totrinnskontroll

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile


@TestConfiguration
class TotrinnskontrollTestConfig {

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var totrinnskontrollRepository: TotrinnskontrollRepository

    @Bean
    @Profile("mock-totrinnkontroll") // Obs! Mock til e2e-tester. Vil ikke fungere som en mock ved manuell testing lokalt.
    @Primary
    fun mockToTrinnKontrollService(): TotrinnskontrollService {
        val totrinnskontrollService: TotrinnskontrollService = mockk()
        every { totrinnskontrollService.besluttTotrinnskontroll(any(), any(), any(), any()) } answers {
            val behandling = firstArg<Behandling>()
            val beslutning = lastArg<Beslutning>()
            behandlingService.oppdaterStatusPåBehandling(behandling.id, if(beslutning.erGodkjent()) BehandlingStatus.IVERKSETTER_VEDTAK else BehandlingStatus.UTREDES )

            val totrinnskontroll = totrinnskontrollRepository.findByBehandlingAndAktiv(behandling.id)!!
            totrinnskontroll.beslutter = "Beslutter"
            totrinnskontroll.beslutterId = "beslutter@nav.no"
            totrinnskontroll.godkjent = beslutning.erGodkjent()
            totrinnskontrollService.lagreEllerOppdater(totrinnskontroll)
        }

        every { totrinnskontrollService.lagreOgDeaktiverGammel(any()) } answers  {
            val totrinnskontroll = firstArg<Totrinnskontroll>()
            val aktivTotrinnskontroll = totrinnskontrollRepository.findByBehandlingAndAktiv(totrinnskontroll.behandling.id)

            if (aktivTotrinnskontroll != null && aktivTotrinnskontroll.id != totrinnskontroll.id) {
                totrinnskontrollRepository.saveAndFlush(aktivTotrinnskontroll.also { it.aktiv = false })
            }

            totrinnskontrollRepository.save(totrinnskontroll)
        }

        every { totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(any(), any()) } answers {
            val behandling = firstArg<Behandling>()
            totrinnskontrollRepository.save(Totrinnskontroll(
                    behandling = behandling,
                    saksbehandler = SikkerhetContext.hentSaksbehandlerNavn(),
                    saksbehandlerId = SikkerhetContext.hentSaksbehandler()
            ))
        }

        every { totrinnskontrollService.hentAktivForBehandling(any()) } answers {
            val behandlingId = firstArg<Long>()
            totrinnskontrollRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)
        }

        every { totrinnskontrollService.lagreEllerOppdater(any()) } answers {
            val totrinnskontroll = firstArg<Totrinnskontroll>()
            totrinnskontrollRepository.save(totrinnskontroll)
        }

        return totrinnskontrollService
    }
}