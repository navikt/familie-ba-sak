package no.nav.familie.ba.sak.toTrinnKontroll

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.vedtak.Beslutning
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.vedtak.producer.MockKafkaProducer.Companion.meldingSendtFor
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
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
@ActiveProfiles("postgres", "mock-dokgen", "mock-pdl", "mock-arbeidsfordeling")
@Tag("integration")
class ToTrinnKontrollTest(
        @Autowired
        private val behandlingService: BehandlingService,

        @Autowired
        private val totrinnskontrollService: TotrinnskontrollService,

        @Autowired
        private val fagsakService: FagsakService
) {

    @Test
    @Tag("integration")
    fun `Skal godkjenne 2 trinnskontroll`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        behandlingService.sendBehandlingTilBeslutter(behandling)
        Assertions.assertEquals(BehandlingStatus.FATTER_VEDTAK, behandlingService.hent(behandling.id).status)
        Assertions.assertEquals(BehandlingStatus.FATTER_VEDTAK.name, (meldingSendtFor(behandling) as BehandlingDVH).behandlingStatus)

        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling = behandling)
        totrinnskontrollService.besluttTotrinnskontroll(behandling, "Beslutter" ,  "beslutterId",  Beslutning.GODKJENT)
        Assertions.assertEquals(BehandlingStatus.IVERKSETTER_VEDTAK, behandlingService.hent(behandling.id).status)
        Assertions.assertEquals(BehandlingStatus.IVERKSETTER_VEDTAK.name, (meldingSendtFor(behandling) as BehandlingDVH).behandlingStatus)

        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandlingId = behandling.id)!!
        Assertions.assertTrue(totrinnskontroll.godkjent)
    }

    @Test
    @Tag("integration")
    fun `Skal underkjenne 2 trinnskontroll`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        behandlingService.sendBehandlingTilBeslutter(behandling)
        Assertions.assertEquals(BehandlingStatus.FATTER_VEDTAK, behandlingService.hent(behandling.id).status)

        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling = behandling)
        totrinnskontrollService.besluttTotrinnskontroll(behandling, "Beslutter", "beslutterId", Beslutning.UNDERKJENT)
        Assertions.assertEquals(BehandlingStatus.UTREDES, behandlingService.hent(behandling.id).status)
        Assertions.assertEquals(BehandlingStatus.UTREDES.name, (meldingSendtFor(behandling) as BehandlingDVH).behandlingStatus)

        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandlingId = behandling.id)!!
        Assertions.assertFalse(totrinnskontroll.godkjent)
    }
}