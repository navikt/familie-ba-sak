package no.nav.familie.ba.sak.behandling.vedtak

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.opprettNyOrdinærBehandling
import no.nav.familie.ba.sak.behandling.vilkår.vilkårsvurderingKomplettForBarnOgSøker
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.familie.ba.sak.util.DbContainerInitializer
import no.nav.familie.ba.sak.util.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.util.randomFnr
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
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
    lateinit var vedtakService: VedtakService

    @Autowired
    lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    @Qualifier("integrasjonClient")
    lateinit var integrasjonClient: IntegrasjonClient

    @Autowired
    lateinit var fagsakService: FagsakService

    @MockK(relaxed = true)
    lateinit var taskRepository: TaskRepository

    @MockK(relaxed = true)
    lateinit var featureToggleService: FeatureToggleService

    lateinit var behandlingService: BehandlingService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        behandlingService = BehandlingService(
                behandlingRepository,
                personopplysningGrunnlagRepository,
                fagsakService,
                integrasjonClient,
                featureToggleService,
                taskRepository)

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
    fun `Skal hente forrige behandling`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        var behandling = opprettNyOrdinærBehandling(fagsak, behandlingService)
        behandling = behandlingService.settVilkårsvurdering(behandling, BehandlingResultat.INNVILGET, "")

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, fnr, barnFnr)
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                personopplysningGrunnlag = personopplysningGrunnlag,
                behandling = behandling,
                restSamletVilkårResultat = vilkårsvurderingKomplettForBarnOgSøker(
                        fnr,
                        listOf(barnFnr)),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )

        val revurderingInnvilgetBehandling =
                behandlingService.lagreNyOgDeaktiverGammelBehandling(Behandling(fagsak = fagsak,
                                                                                journalpostID = null,
                                                                                type = BehandlingType.REVURDERING,
                                                                                resultat = BehandlingResultat.INNVILGET,
                                                                                kategori = BehandlingKategori.NASJONAL,
                                                                                underkategori = BehandlingUnderkategori.ORDINÆR))


        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                personopplysningGrunnlag = personopplysningGrunnlag,
                behandling = revurderingInnvilgetBehandling,
                restSamletVilkårResultat = vilkårsvurderingKomplettForBarnOgSøker(
                        fnr,
                        listOf(barnFnr)),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )


        val revurderingOpphørBehandling =
                behandlingService.lagreNyOgDeaktiverGammelBehandling(Behandling(fagsak = fagsak,
                                                                                journalpostID = null,
                                                                                type = BehandlingType.REVURDERING,
                                                                                resultat = BehandlingResultat.OPPHØRT,
                                                                                kategori = BehandlingKategori.NASJONAL,
                                                                                underkategori = BehandlingUnderkategori.ORDINÆR))

        val forrigeVedtak = vedtakService.hentForrigeVedtak(revurderingOpphørBehandling)
        Assertions.assertNotNull(forrigeVedtak)
        Assertions.assertEquals(revurderingInnvilgetBehandling.id, forrigeVedtak?.behandling?.id)
    }

}