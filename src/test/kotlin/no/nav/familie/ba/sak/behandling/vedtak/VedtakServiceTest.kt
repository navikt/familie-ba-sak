package no.nav.familie.ba.sak.behandling.vedtak

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.mockk.MockKAnnotations
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.fagsak.FagsakPersonRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.vilkår.*
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
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
@ActiveProfiles("postgres", "mock-dokgen", "mock-oauth", "mock-pdl")
@Tag("integration")
@AutoConfigureWireMock(port = 28085)
class VedtakServiceTest(
        @Autowired
        val behandlingRepository: BehandlingRepository,

        @Autowired
        val behandlingResultatService: BehandlingResultatService,

        @Autowired
        val vedtakService: VedtakService,

        @Autowired
        val persongrunnlagService: PersongrunnlagService,

        @Autowired
        val beregningService: BeregningService,

        @Autowired
        val fagsakService: FagsakService,

        @Autowired
        val fagsakPersonRepository: FagsakPersonRepository,

        @Autowired
        val totrinnskontrollService: TotrinnskontrollService,

        @Autowired
        val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,

        @Autowired
        val loggService: LoggService
) {

    lateinit var behandlingService: BehandlingService
    lateinit var vilkårResultat1: VilkårResultat
    lateinit var vilkårResultat2: VilkårResultat
    lateinit var vilkårResultat3: VilkårResultat
    lateinit var behandlingResultat: BehandlingResultat
    lateinit var personResultat: PersonResultat
    lateinit var vilkår: Vilkår
    lateinit var resultat: Resultat
    lateinit var behandling: Behandling

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        behandlingService = BehandlingService(
                behandlingRepository,
                fagsakPersonRepository,
                persongrunnlagService,
                beregningService,
                fagsakService,
                loggService)

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

        val personIdent = randomFnr()

        behandling = lagBehandling()

        vilkår = Vilkår.LOVLIG_OPPHOLD
        resultat = Resultat.JA

        behandlingResultat = lagBehandlingResultat(personIdent, behandling, resultat)

        personResultat = PersonResultat(
                behandlingResultat = behandlingResultat,
                personIdent = personIdent
        )

        vilkårResultat1 = VilkårResultat(1, personResultat, vilkår, resultat,
                                         LocalDate.of(2010, 1, 1), LocalDate.of(2010, 6, 1),
                                         "", behandlingResultat.behandling.id, regelInput = null, regelOutput = null)
        vilkårResultat2 = VilkårResultat(2, personResultat, vilkår, resultat,
                                         LocalDate.of(2010, 6, 2), LocalDate.of(2010, 8, 1),
                                         "", behandlingResultat.behandling.id, regelInput = null, regelOutput = null)
        vilkårResultat3 = VilkårResultat(3, personResultat, vilkår, resultat,
                                         LocalDate.of(2010, 8, 2), LocalDate.of(2010, 12, 1),
                                         "", behandlingResultat.behandling.id, regelInput = null, regelOutput = null)
        personResultat.setVilkårResultater(setOf(vilkårResultat1,
                                                 vilkårResultat2,
                                                 vilkårResultat3).toSortedSet(PersonResultat.comparator))
    }

    @Test
    @Tag("integration")
    fun `Opprett innvilget behandling med vedtak`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val behandlingResultat = lagBehandlingResultat(fnr, behandling, Resultat.JA)

        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat = behandlingResultat, loggHendelse = true)

        val behandlingResultatType =
                behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandlingId = behandling.id)
        Assertions.assertNotNull(behandling.fagsak.id)
        Assertions.assertEquals(behandlingResultatType, BehandlingResultatType.INNVILGET)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag
        )

        totrinnskontrollService.opprettEllerHentTotrinnskontroll(behandling, "ansvarligSaksbehandler")
        totrinnskontrollService.besluttTotrinnskontroll(behandling, "ansvarligBeslutter", Beslutning.GODKJENT)

        val hentetVedtak = vedtakService.hentAktivForBehandling(behandling.id)
        Assertions.assertNotNull(hentetVedtak)
        Assertions.assertNull(hentetVedtak!!.vedtaksdato)
        Assertions.assertEquals(null, hentetVedtak.stønadBrevPdF)

        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandlingId = behandling.id)
        Assertions.assertNotNull(totrinnskontroll)
        Assertions.assertEquals("ansvarligSaksbehandler", totrinnskontroll!!.saksbehandler)
    }

    @Test
    @Tag("integration")
    fun `Opprett opphørt behandling med vedtak`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val behandlingResultat = lagBehandlingResultat(fnr, behandling, Resultat.NEI)

        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat = behandlingResultat, loggHendelse = true)

        val behandlingResultatType =
                behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandlingId = behandling.id)
        Assertions.assertNotNull(behandling.fagsak.id)
        Assertions.assertEquals(behandlingResultatType, BehandlingResultatType.AVSLÅTT)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag
        )

        val hentetVedtak = vedtakService.hentAktivForBehandling(behandling.id)
        Assertions.assertNotNull(hentetVedtak)
    }

    @Test
    fun `Skal hente forrige behandling`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                personopplysningGrunnlag = personopplysningGrunnlag,
                behandling = behandling
        )

        val revurderingInnvilgetBehandling =
                behandlingService.lagreNyOgDeaktiverGammelBehandling(Behandling(fagsak = fagsak,
                                                                                type = BehandlingType.REVURDERING,
                                                                                kategori = BehandlingKategori.NASJONAL,
                                                                                underkategori = BehandlingUnderkategori.ORDINÆR,
                                                                                opprinnelse = BehandlingOpprinnelse.MANUELL))


        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                personopplysningGrunnlag = personopplysningGrunnlag,
                behandling = revurderingInnvilgetBehandling
        )


        val revurderingOpphørBehandling =
                behandlingService.lagreNyOgDeaktiverGammelBehandling(Behandling(fagsak = fagsak,
                                                                                type = BehandlingType.REVURDERING,
                                                                                kategori = BehandlingKategori.NASJONAL,
                                                                                underkategori = BehandlingUnderkategori.ORDINÆR,
                                                                                opprinnelse = BehandlingOpprinnelse.MANUELL))

        val forrigeVedtak = vedtakService.hentForrigeVedtakPåFagsak(revurderingOpphørBehandling)
        Assertions.assertNotNull(forrigeVedtak)
        Assertions.assertEquals(revurderingInnvilgetBehandling.id, forrigeVedtak?.behandling?.id)
    }


    @Test
    @Tag("integration")
    fun `Opprett 2 vedtak og se at det siste vedtaket får aktiv satt til true`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        opprettNyttInvilgetVedtak(behandling, ansvarligEnhet = "ansvarligEnhet1")
        opprettNyttInvilgetVedtak(behandling, ansvarligEnhet = "ansvarligEnhet2")

        val hentetVedtak = vedtakService.hentAktivForBehandling(behandling.id)
        Assertions.assertNotNull(hentetVedtak)
        Assertions.assertEquals("ansvarligEnhet2", hentetVedtak?.ansvarligEnhet)
    }

    private fun opprettNyttInvilgetVedtak(behandling: Behandling, ansvarligEnhet: String = "ansvarligEnhet"): Vedtak {
        vedtakService.lagreOgDeaktiverGammel(Vedtak(behandling = behandling,
                                                    ansvarligEnhet = ansvarligEnhet,
                                                    vedtaksdato = LocalDate.now())
        )

        return vedtakService.hentAktivForBehandling(behandling.id)!!
    }
}