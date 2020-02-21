package no.nav.familie.ba.sak.toTrinnKontroll

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.util.DbContainerInitializer
import no.nav.familie.ba.sak.util.randomFnr
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

    @Test
    @Tag("integration")
    fun `Skal validere 2 trinnskontroll`() {
        val fnr = randomFnr()

        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       "sdf",
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       BehandlingKategori.NASJONAL,
                                                                       BehandlingUnderkategori.ORDINÆR)

        behandlingService.sendBehandlingTilBeslutter(behandling)
        Assertions.assertEquals(BehandlingStatus.SENDT_TIL_BESLUTTER, behandlingService.hentBehandling(behandling.id)?.status)

        behandlingService.valider2trinnVedIverksetting(behandling, "beslutter")
        Assertions.assertEquals(BehandlingStatus.SENDT_TIL_IVERKSETTING, behandlingService.hentBehandling(behandling.id)?.status)
    }

    @Test
    @Tag("integration")
    fun `Skal kaste feil ved lik fatter og beslutter`() {
        val fnr = randomFnr()

        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       "sdf",
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       BehandlingKategori.NASJONAL,
                                                                       BehandlingUnderkategori.ORDINÆR)

        behandlingService.sendBehandlingTilBeslutter(behandling)
        Assertions.assertEquals(BehandlingStatus.SENDT_TIL_BESLUTTER, behandlingService.hentBehandling(behandling.id)?.status)

        assertThrows<IllegalArgumentException> { behandlingService.valider2trinnVedIverksetting (behandling, "beslutter") }
    }
}