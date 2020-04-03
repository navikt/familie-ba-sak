package no.nav.familie.ba.sak.behandling.vedtak

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.mockk.MockKAnnotations
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.beregning.BeregningService
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
class VedtakServiceTest {

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var behandlingResultatService: BehandlingResultatService

    @Autowired
    lateinit var vedtakService: VedtakService

    @Autowired
    lateinit var persongrunnlagService: PersongrunnlagService

    @Autowired
    lateinit var beregningService: BeregningService

    @Autowired
    lateinit var fagsakService: FagsakService

    lateinit var behandlingService: BehandlingService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        behandlingService = BehandlingService(
                behandlingRepository,
                persongrunnlagService,
                beregningService,
                fagsakService)

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
    fun `Opprett innvilget behandling med vedtak`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        var behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val behandlingResultat = lagBehandlingResultat(fnr, behandling, Resultat.JA)
        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat)

        val behandlingResultatType =
                behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandlingId = behandling.id)
        Assertions.assertNotNull(behandling.fagsak.id)
        Assertions.assertEquals(behandlingResultatType, BehandlingResultatType.INNVILGET)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )

        val hentetVedtak = vedtakService.hentAktivForBehandling(behandling.id)
        Assertions.assertNotNull(hentetVedtak)
        Assertions.assertEquals("ansvarligSaksbehandler", hentetVedtak?.ansvarligSaksbehandler)
        Assertions.assertEquals("", hentetVedtak?.stønadBrevMarkdown)
    }

    @Test
    @Tag("integration")
    fun `Opprett opphørt behandling med vedtak`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        var behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val behandlingResultat = lagBehandlingResultat(fnr, behandling, Resultat.NEI)

        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat)

        val behandlingResultatType =
                behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandlingId = behandling.id)
        Assertions.assertNotNull(behandling.fagsak.id)
        Assertions.assertEquals(behandlingResultatType, BehandlingResultatType.AVSLÅTT)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )

        val hentetVedtak = vedtakService.hentAktivForBehandling(behandling.id)
        Assertions.assertNotNull(hentetVedtak)
        Assertions.assertEquals("ansvarligSaksbehandler", hentetVedtak?.ansvarligSaksbehandler)
        Assertions.assertNotEquals("", hentetVedtak?.stønadBrevMarkdown)
    }

    @Test
    fun `Skal hente forrige behandling`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        var behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                personopplysningGrunnlag = personopplysningGrunnlag,
                behandling = behandling,
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )

        val revurderingInnvilgetBehandling =
                behandlingService.lagreNyOgDeaktiverGammelBehandling(Behandling(fagsak = fagsak,
                                                                                journalpostID = null,
                                                                                type = BehandlingType.REVURDERING,
                                                                                kategori = BehandlingKategori.NASJONAL,
                                                                                underkategori = BehandlingUnderkategori.ORDINÆR))


        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                personopplysningGrunnlag = personopplysningGrunnlag,
                behandling = revurderingInnvilgetBehandling,
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )


        val revurderingOpphørBehandling =
                behandlingService.lagreNyOgDeaktiverGammelBehandling(Behandling(fagsak = fagsak,
                                                                                journalpostID = null,
                                                                                type = BehandlingType.REVURDERING,
                                                                                kategori = BehandlingKategori.NASJONAL,
                                                                                underkategori = BehandlingUnderkategori.ORDINÆR))

        val forrigeVedtak = vedtakService.hentForrigeVedtak(revurderingOpphørBehandling)
        Assertions.assertNotNull(forrigeVedtak)
        Assertions.assertEquals(revurderingInnvilgetBehandling.id, forrigeVedtak?.behandling?.id)
    }

    @Test
    @Tag("integration")
    fun `Opprett 2 vedtak og se at det siste vedtaket får aktiv satt til true`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        opprettNyttInvilgetVedtak(behandling, saksbehandler = "ansvarligSaksbehandler1")
        opprettNyttInvilgetVedtak(behandling, saksbehandler = "ansvarligSaksbehandler2")

        val hentetVedtak = vedtakService.hentAktivForBehandling(behandling.id)
        Assertions.assertNotNull(hentetVedtak)
        Assertions.assertEquals("ansvarligSaksbehandler2", hentetVedtak?.ansvarligSaksbehandler)
    }

    private fun opprettNyttInvilgetVedtak(behandling: Behandling, saksbehandler: String = "ansvarligSaksbehandler"): Vedtak {
        vedtakService.lagreOgDeaktiverGammel(Vedtak(behandling = behandling,
                                                    ansvarligSaksbehandler = saksbehandler,
                                                    vedtaksdato = LocalDate.now())
        )

        return vedtakService.hentAktivForBehandling(behandling.id)!!
    }
}