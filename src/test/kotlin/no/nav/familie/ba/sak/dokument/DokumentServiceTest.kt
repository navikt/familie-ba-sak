package no.nav.familie.ba.sak.dokument

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.tomakehurst.wiremock.client.WireMock.*
import io.mockk.MockKAnnotations
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.Beslutning
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.Vilkårsvurdering
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.TEST_PDF
import no.nav.familie.ba.sak.dokument.domene.BrevType
import no.nav.familie.ba.sak.dokument.domene.maler.Innvilget
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@SpringBootTest(properties = ["FAMILIE_INTEGRASJONER_API_URL=http://localhost:28085/api"])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-dokgen-klient", "mock-økonomi", "mock-oauth", "mock-pdl", "mock-task-repository")
@Tag("integration")
@AutoConfigureWireMock(port = 28085)
class DokumentServiceTest(
        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val behandlingService: BehandlingService,

        @Autowired
        private val beregningService: BeregningService,

        @Autowired
        private val vilkårsvurderingService: VilkårsvurderingService,

        @Autowired
        private val persongrunnlagService: PersongrunnlagService,

        @Autowired
        private val vedtakService: VedtakService,

        @Autowired
        private val dokumentService: DokumentService,

        @Autowired
        private val totrinnskontrollService: TotrinnskontrollService,

        @Autowired
        private val stegService: StegService,

        @Autowired
        private val malerService: MalerService,

        @Autowired
        private val databaseCleanupService: DatabaseCleanupService,

        @Autowired
        private val integrasjonClient: IntegrasjonClient
) {

    @BeforeEach
    fun setup() {
        databaseCleanupService.truncate()
        MockKAnnotations.init(this)

        stubFor(get(urlEqualTo("/api/aktoer/v1"))
                        .willReturn(aResponse()
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(objectMapper.writeValueAsString(Ressurs.success(mapOf("aktørId" to "1"))))))

        stubFor(get(urlEqualTo("/api/personopplysning/v1/info/BAR"))
                        .willReturn(aResponse()
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(objectMapper.writeValueAsString(Ressurs.success(PersonInfo(
                                                    LocalDate.of(2019,
                                                                 1,
                                                                 1)))))))
    }

    @Test
    @Tag("integration")
    fun `Hent vedtaksbrev`() {
        val fnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        assertNotNull(behandling.fagsak.id)
        assertNotNull(behandling.id)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barn1Fnr, barn2Fnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                personopplysningGrunnlag = personopplysningGrunnlag,
                behandling = behandling
        )

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
        assertNotNull(vedtak)

        val dato_2020_01_01 = LocalDate.of(2020, 1, 1)
        val stønadTom = dato_2020_01_01.plusYears(17)
        val vilkårsvurdering =
                Vilkårsvurdering(behandling = behandling)
        vilkårsvurdering.personResultater = lagPersonResultaterForSøkerOgToBarn(vilkårsvurdering,
                                                                                fnr,
                                                                                barn1Fnr,
                                                                                barn2Fnr,
                                                                                dato_2020_01_01.minusMonths(1),
                                                                                stønadTom)
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
        val nyBehandlingResultatType = vilkårsvurdering.beregnSamletResultat(personopplysningGrunnlag, behandling)
        vilkårsvurdering.oppdaterSamletResultat(nyBehandlingResultatType)
        vilkårsvurderingService.oppdater(vilkårsvurdering)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling, "ansvarligSaksbehandler")
        totrinnskontrollService.besluttTotrinnskontroll(behandling, "ansvarligBeslutter", Beslutning.GODKJENT)

        vedtakService.oppdaterVedtakMedStønadsbrev(vedtak!!)

        val pdfvedtaksbrevRess = dokumentService.hentBrevForVedtak(vedtak)
        Assertions.assertEquals(Ressurs.Status.SUKSESS, pdfvedtaksbrevRess.status)
        assert(pdfvedtaksbrevRess.data!!.contentEquals(TEST_PDF))
    }

    @Test
    @Tag("integration")
    fun `generer vedtaksbrev`() {
        val fnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        assertNotNull(behandling.fagsak.id)
        assertNotNull(behandling.id)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barn1Fnr, barn2Fnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)
        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                personopplysningGrunnlag = personopplysningGrunnlag,
                behandling = behandling
        )
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
        assertNotNull(vedtak)

        assertThrows<Feil> {
            dokumentService.genererBrevForVedtak(vedtak!!)
        }

        val dato_2020_01_01 = LocalDate.of(2020, 1, 1)
        val stønadTom = dato_2020_01_01.plusYears(17)
        val vilkårsvurdering =
                Vilkårsvurdering(behandling = behandling)
        vilkårsvurdering.personResultater = lagPersonResultaterForSøkerOgToBarn(vilkårsvurdering,
                                                                                fnr,
                                                                                barn1Fnr,
                                                                                barn2Fnr,
                                                                                dato_2020_01_01.minusMonths(1),
                                                                                stønadTom)
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
        val nyBehandlingResultatType = vilkårsvurdering.beregnSamletResultat(personopplysningGrunnlag, behandling)
        vilkårsvurdering.oppdaterSamletResultat(nyBehandlingResultatType)
        vilkårsvurderingService.oppdater(vilkårsvurdering)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling, "ansvarligSaksbehandler")
        totrinnskontrollService.besluttTotrinnskontroll(behandling, "ansvarligBeslutter", Beslutning.GODKJENT)

        vedtakService.oppdaterVedtakMedStønadsbrev(vedtak!!)

        val pdfvedtaksbrev = dokumentService.genererBrevForVedtak(vedtak)
        assert(pdfvedtaksbrev.contentEquals(TEST_PDF))
    }

    @Test
    fun `Skal verifisere at brev får riktig signatur ved alle steg i behandling`() {
        val mockSaksbehandler = "Mock Saksbehandler"
        val mockBeslutter = "Mock Beslutter"

        val behandlingEtterVilkårsvurderingSteg = kjørStegprosessForFGB(
                tilSteg = StegType.VILKÅRSVURDERING,
                søkerFnr = ClientMocks.søkerFnr[0],
                barnasIdenter = listOf(ClientMocks.barnFnr[0]),
                fagsakService = fagsakService,
                behandlingService = behandlingService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService
        )
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandlingEtterVilkårsvurderingSteg.id)!!

        val mal = malerService.mapTilVedtakBrevfelter(
                vedtak = vedtak,
                behandlingResultatType = BehandlingResultatType.INNVILGET
        )

        val innvilgetData = objectMapper.readValue<Innvilget>(mal.fletteFelter)

        assertEquals("NAV Familie- og pensjonsytelser Oslo 1", innvilgetData.enhet)
        assertEquals("System", innvilgetData.saksbehandler)
        assertEquals("Beslutter", innvilgetData.beslutter)

        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandlingEtterVilkårsvurderingSteg, mockSaksbehandler)
        val behandlingEtterSendTilBeslutter =
                behandlingEtterVilkårsvurderingSteg.leggTilBehandlingStegTilstand(StegType.BESLUTTE_VEDTAK)
        behandlingService.lagreEllerOppdater(behandlingEtterSendTilBeslutter)

        val vedtakEtterSendTilBeslutter =
                vedtakService.hentAktivForBehandling(behandlingId = behandlingEtterSendTilBeslutter.id)!!

        val malEtterSendTilBeslutter = malerService.mapTilVedtakBrevfelter(
                vedtak = vedtakEtterSendTilBeslutter,
                behandlingResultatType = BehandlingResultatType.INNVILGET
        )

        val innvilgetDataEtterSendTilBeslutter = objectMapper.readValue<Innvilget>(malEtterSendTilBeslutter.fletteFelter)

        assertEquals(mockSaksbehandler, innvilgetDataEtterSendTilBeslutter.saksbehandler)
        assertEquals("System", innvilgetDataEtterSendTilBeslutter.beslutter)

        totrinnskontrollService.besluttTotrinnskontroll(behandling = behandlingEtterSendTilBeslutter,
                                                        beslutter = mockBeslutter,
                                                        beslutning = Beslutning.GODKJENT)
        val behandlingEtterVedtakBesluttet =
                behandlingEtterVilkårsvurderingSteg.leggTilBehandlingStegTilstand(StegType.IVERKSETT_MOT_OPPDRAG)

        val vedtakEtterVedtakBesluttet = vedtakService.hentAktivForBehandling(behandlingId = behandlingEtterVedtakBesluttet.id)!!

        val malEtterVedtakBesluttet = malerService.mapTilVedtakBrevfelter(
                vedtak = vedtakEtterVedtakBesluttet,
                behandlingResultatType = BehandlingResultatType.INNVILGET
        )

        val innvilgetDataEtterVedtakBesluttet = objectMapper.readValue<Innvilget>(malEtterVedtakBesluttet.fletteFelter)

        assertEquals(mockSaksbehandler, innvilgetDataEtterVedtakBesluttet.saksbehandler)
        assertEquals(mockBeslutter, innvilgetDataEtterVedtakBesluttet.beslutter)
    }

    @Test
    fun `Skal verifisere at man ikke får generert brev etter at behandlingen er sendt fra beslutter`() {
        val behandlingEtterVedtakBesluttet = kjørStegprosessForFGB(
                tilSteg = StegType.BESLUTTE_VEDTAK,
                søkerFnr = ClientMocks.søkerFnr[0],
                barnasIdenter = listOf(ClientMocks.barnFnr[0]),
                fagsakService = fagsakService,
                behandlingService = behandlingService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService
        )

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandlingEtterVedtakBesluttet.id)!!
        val feil = assertThrows<Feil> {
            dokumentService.genererBrevForVedtak(vedtak)
        }
        assertEquals("Klarte ikke generere vedtaksbrev: Ikke tillatt å generere brev etter at behandlingen er sendt fra beslutter",
                     feil.message)
    }

    @Test
    fun `Sjekk at send brev for trukket søknad ikke genererer forside`() {
        val fnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barn1Fnr, barn2Fnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val manueltBrevRequest = DokumentController.ManueltBrevRequest(brevmal = BrevType.HENLEGGE_TRUKKET_SØKNAD,
                                                                       mottakerIdent = fnr)
        dokumentService.sendManueltBrev(behandling, manueltBrevRequest)

        io.mockk.verify(exactly = 1) {
            integrasjonClient.journalførManueltBrev(fnr = manueltBrevRequest.mottakerIdent,
                                                    fagsakId = behandling.fagsak.id.toString(),
                                                    journalførendeEnhet = any(),
                                                    brev = any(),
                                                    førsteside = null,
                                                    brevType = manueltBrevRequest.brevmal.arkivType)
        }
    }
}