package no.nav.familie.ba.sak.dokument

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.mockk.MockKAnnotations
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.PersonBeregning
import no.nav.familie.ba.sak.beregning.NyBeregning
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
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
@ActiveProfiles("postgres", "mock-dokgen", "mock-oauth")
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
        private val dokumentService: DokumentService
) {

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)

        stubFor(get(urlEqualTo("/api/aktoer/v1"))
                        .willReturn(aResponse()
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(objectMapper.writeValueAsString(Ressurs.success(mapOf("aktørId" to "1"))))))
        stubFor(get(urlEqualTo("/api/personopplysning/v1/info"))
                        .willReturn(aResponse()
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(objectMapper.writeValueAsString(Ressurs.success(Personinfo(
                                                    LocalDate.of(2019,
                                                                 1,
                                                                 1)))))))
        stubFor(get(urlEqualTo("/api/personopplysning/v1/info/BAR"))
                        .willReturn(aResponse()
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(objectMapper.writeValueAsString(Ressurs.success(Personinfo(
                                                    LocalDate.of(2019,
                                                                 1,
                                                                 1)))))))
    }

    @Test
    @Tag("integration")
    fun `Hent HTML vedtaksbrev`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val behandlingResultat = lagBehandlingResultat(fnr, behandling, Resultat.JA)

        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat)

        Assertions.assertNotNull(behandling.fagsak.id)
        Assertions.assertNotNull(behandling.id)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                personopplysningGrunnlag = personopplysningGrunnlag,
                behandling = behandling,
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
        Assertions.assertNotNull(vedtak)

        val nyBeregning = NyBeregning(
                listOf(PersonBeregning(ident = barnFnr,
                                       beløp = 1054,
                                       stønadFom = LocalDate.of(
                                               2020,
                                               1,
                                               1),
                                       ytelsetype = Ytelsetype.ORDINÆR_BARNETRYGD))
        )

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag, nyBeregning)
        vedtakService.oppdaterVedtakMedStønadsbrev(vedtak!!)

        val htmlvedtaksbrevRess = dokumentService.hentHtmlForVedtak(vedtak.id)
        Assertions.assertEquals(Ressurs.Status.SUKSESS, htmlvedtaksbrevRess.status)
        assert(htmlvedtaksbrevRess.data!! == "<HTML>HTML_MOCKUP</HTML>")
    }
}