package no.nav.familie.ba.sak.toTrinnKontroll

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.util.DbContainerInitializer
import no.nav.familie.ba.sak.util.randomFnr
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
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
    lateinit var behandlingRepository: BehandlingRepository

    @MockBean
    lateinit var integrasjonTjeneste: IntegrasjonTjeneste

    @BeforeEach
    fun setup() {
        Mockito.`when`(integrasjonTjeneste.hentAktørId(ArgumentMatchers.anyString())).thenReturn(AktørId("1"))
    }

    @Test
    @Tag("integration")
    fun `Skal validere 2 trinnskontroll`() {
        val fnr = randomFnr()

        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       null,
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       BehandlingKategori.NASJONAL,
                                                                       BehandlingUnderkategori.ORDINÆR)

        behandlingService.sendBehandlingTilBeslutter(behandling)
        Assertions.assertEquals(BehandlingStatus.SENDT_TIL_BESLUTTER, behandlingService.hentBehandling(behandling.id)?.status)

        behandlingService.valider2trinnVedIverksetting(behandling, "beslutter")
        Assertions.assertEquals(BehandlingStatus.GODKJENT, behandlingService.hentBehandling(behandling.id)?.status)
    }

    @Test
    @Tag("integration")
    fun `Skal kaste feil ved lik fatter og beslutter`() {
        val fnr = randomFnr()

        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       null,
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       BehandlingKategori.NASJONAL,
                                                                       BehandlingUnderkategori.ORDINÆR)

        behandling.status = BehandlingStatus.SENDT_TIL_BESLUTTER
        behandlingRepository.saveAndFlush(behandling)

        val endretBehandling = behandlingService.hentBehandling(behandling.id)
        Assertions.assertEquals(BehandlingStatus.SENDT_TIL_BESLUTTER, endretBehandling?.status)
        Assertions.assertNotNull(endretBehandling?.endretAv)

        assertThrows<IllegalStateException> { behandlingService.valider2trinnVedIverksetting (endretBehandling!!, "VL") }
    }
}