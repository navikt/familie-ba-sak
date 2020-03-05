package no.nav.familie.ba.sak.dokument

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.mockk.MockKAnnotations
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.beregning.BarnBeregning
import no.nav.familie.ba.sak.beregning.NyBeregning
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.opprettNyOrdinærBehandling
import no.nav.familie.ba.sak.behandling.vedtak.NyttVedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakResultat
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.familie.ba.sak.behandling.vilkår.vilkårsvurderingKomplettForBarnOgSøker
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.familie.ba.sak.util.DbContainerInitializer
import no.nav.familie.ba.sak.util.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.util.randomFnr
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
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
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,

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
        val behandling = opprettNyOrdinærBehandling(fagsak, behandlingService)

        Assertions.assertNotNull(behandling.fagsak.id)
        Assertions.assertNotNull(behandling.id)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, fnr, barnFnr)
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        vedtakService.nyttVedtakForAktivBehandling(
                personopplysningGrunnlag = personopplysningGrunnlag,
                behandling = behandling,
                nyttVedtak = NyttVedtak(resultat = VedtakResultat.INNVILGET,
                                        samletVilkårResultat = vilkårsvurderingKomplettForBarnOgSøker(
                                                fnr,
                                                listOf(barnFnr)),
                                        begrunnelse = ""),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
        Assertions.assertNotNull(vedtak)

        vedtakService.oppdaterAktivVedtakMedBeregning(
                vedtak = vedtak!!,
                personopplysningGrunnlag = personopplysningGrunnlag,
                nyBeregning = NyBeregning(
                        listOf(BarnBeregning(ident = fnr,
                                              beløp = 1054,
                                              stønadFom = LocalDate.of(
                                                      2020,
                                                      1,
                                                      1),
                                              ytelsetype = Ytelsetype.ORDINÆR_BARNETRYGD))
                )
        )

        val htmlvedtaksbrevRess = dokumentService.hentHtmlVedtakForBehandling(behandling.id)
        Assertions.assertEquals(Ressurs.Status.SUKSESS, htmlvedtaksbrevRess.status)
        assert(htmlvedtaksbrevRess.data!! == "<HTML>HTML_MOCKUP</HTML>")
    }
}