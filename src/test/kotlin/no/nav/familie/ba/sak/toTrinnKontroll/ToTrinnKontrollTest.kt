package no.nav.familie.ba.sak.toTrinnKontroll

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.ToTrinnKontrollService
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
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
    lateinit var toTrinnKontrollService: ToTrinnKontrollService

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var fagsakService: FagsakService

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

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       null,
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       BehandlingKategori.NASJONAL,
                                                                       BehandlingUnderkategori.ORDINÆR)

        behandlingService.sendBehandlingTilBeslutter(behandling)
        Assertions.assertEquals(BehandlingStatus.SENDT_TIL_BESLUTTER, behandlingService.hent(behandling.id).status)

        toTrinnKontrollService.valider2trinnVedIverksetting(behandling, "beslutter")
        Assertions.assertEquals(BehandlingStatus.GODKJENT, behandlingService.hent(behandling.id).status)
    }

    @Test
    @Tag("integration")
    fun `Skal kaste feil ved lik fatter og beslutter`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       null,
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       BehandlingKategori.NASJONAL,
                                                                       BehandlingUnderkategori.ORDINÆR)

        behandling.status = BehandlingStatus.SENDT_TIL_BESLUTTER
        behandlingRepository.saveAndFlush(behandling)

        val endretBehandling = behandlingService.hent(behandling.id)
        Assertions.assertEquals(BehandlingStatus.SENDT_TIL_BESLUTTER, endretBehandling.status)
        Assertions.assertNotNull(endretBehandling.endretAv)

        assertThrows<IllegalStateException> { toTrinnKontrollService.valider2trinnVedIverksetting (endretBehandling, "VL") }
    }
}