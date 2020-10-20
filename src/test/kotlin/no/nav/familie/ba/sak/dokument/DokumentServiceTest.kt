package no.nav.familie.ba.sak.dokument

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.mockk.MockKAnnotations
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.Beslutning
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultat
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.config.TEST_PDF
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.*
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
@ActiveProfiles("postgres", "mock-dokgen-klient", "mock-økonomi", "mock-oauth", "mock-pdl", "mock-arbeidsfordeling")
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
        private val behandlingResultatService: BehandlingResultatService,

        @Autowired
        private val persongrunnlagService: PersongrunnlagService,

        @Autowired
        private val vedtakService: VedtakService,

        @Autowired
        private val dokumentService: DokumentService,

        @Autowired
        private val totrinnskontrollService: TotrinnskontrollService,

        @Autowired
        private val databaseCleanupService: DatabaseCleanupService
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

        Assertions.assertNotNull(behandling.fagsak.id)
        Assertions.assertNotNull(behandling.id)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barn1Fnr, barn2Fnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                personopplysningGrunnlag = personopplysningGrunnlag,
                behandling = behandling
        )

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
        Assertions.assertNotNull(vedtak)

        val dato_2020_01_01 = LocalDate.of(2020, 1, 1)
        val stønadTom = dato_2020_01_01.plusYears(17)
        val behandlingResultat =
                BehandlingResultat(behandling = behandling)
        behandlingResultat.personResultater = lagPersonResultaterForSøkerOgToBarn(behandlingResultat,
                                                                                  fnr,
                                                                                  barn1Fnr,
                                                                                  barn2Fnr,
                                                                                  dato_2020_01_01.minusMonths(1),
                                                                                  stønadTom)
        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat = behandlingResultat)
        val nyBehandlingResultatType = behandlingResultat.beregnSamletResultat(personopplysningGrunnlag, behandling)
        behandlingResultat.oppdaterSamletResultat(nyBehandlingResultatType)
        behandlingResultatService.oppdater(behandlingResultat)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
        totrinnskontrollService.opprettEllerHentTotrinnskontroll(behandling, "ansvarligSaksbehandler")
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

        Assertions.assertNotNull(behandling.fagsak.id)
        Assertions.assertNotNull(behandling.id)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barn1Fnr, barn2Fnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)
        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                personopplysningGrunnlag = personopplysningGrunnlag,
                behandling = behandling
        )
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
        Assertions.assertNotNull(vedtak)

        assertThrows<Feil> {
            dokumentService.genererBrevForVedtak(vedtak!!)
        }

        val dato_2020_01_01 = LocalDate.of(2020, 1, 1)
        val stønadTom = dato_2020_01_01.plusYears(17)
        val behandlingResultat =
                BehandlingResultat(behandling = behandling)
        behandlingResultat.personResultater = lagPersonResultaterForSøkerOgToBarn(behandlingResultat,
                                                                                  fnr,
                                                                                  barn1Fnr,
                                                                                  barn2Fnr,
                                                                                  dato_2020_01_01.minusMonths(1),
                                                                                  stønadTom)
        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat = behandlingResultat)
        val nyBehandlingResultatType = behandlingResultat.beregnSamletResultat(personopplysningGrunnlag, behandling)
        behandlingResultat.oppdaterSamletResultat(nyBehandlingResultatType)
        behandlingResultatService.oppdater(behandlingResultat)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        totrinnskontrollService.opprettEllerHentTotrinnskontroll(behandling, "ansvarligSaksbehandler")
        totrinnskontrollService.besluttTotrinnskontroll(behandling, "ansvarligBeslutter", Beslutning.GODKJENT)

        vedtakService.oppdaterVedtakMedStønadsbrev(vedtak!!)

        val pdfvedtaksbrev = dokumentService.genererBrevForVedtak(vedtak)
        assert(pdfvedtaksbrev.contentEquals(TEST_PDF))
    }
}