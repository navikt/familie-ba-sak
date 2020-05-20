package no.nav.familie.ba.sak.toTrinnKontroll

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.ToTrinnKontrollService
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.vedtak.Beslutning
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomFnr
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-dokgen")
@Tag("integration")
class ToTrinnKontrollTest {

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var toTrinnKontrollService: ToTrinnKontrollService

    @Autowired
    lateinit var fagsakService: FagsakService

    @Test
    @Tag("integration")
    fun `Skal godkjenne 2 trinnskontroll`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        behandlingService.sendBehandlingTilBeslutter(behandling)
        Assertions.assertEquals(BehandlingStatus.SENDT_TIL_BESLUTTER, behandlingService.hent(behandling.id).status)

        toTrinnKontrollService.valider2trinnVedBeslutningOmIverksetting(behandling, "beslutter", beslutning = Beslutning.GODKJENT)
        Assertions.assertEquals(BehandlingStatus.GODKJENT, behandlingService.hent(behandling.id).status)
    }

    @Test
    @Tag("integration")
    fun `Skal underkjenne 2 trinnskontroll`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        behandlingService.sendBehandlingTilBeslutter(behandling)
        Assertions.assertEquals(BehandlingStatus.SENDT_TIL_BESLUTTER, behandlingService.hent(behandling.id).status)

        toTrinnKontrollService.valider2trinnVedBeslutningOmIverksetting(behandling, "beslutter", beslutning = Beslutning.UNDERKJENT)
        Assertions.assertEquals(BehandlingStatus.UNDERKJENT_AV_BESLUTTER, behandlingService.hent(behandling.id).status)
    }
}