package no.nav.familie.ba.sak.behandling.vedtak

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.mockk.MockKAnnotations
import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.BehandlingMetrikker
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.fagsak.FagsakPersonRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.RestPostVedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat.Companion.VilkårResultatComparator
import no.nav.familie.ba.sak.behandling.vilkår.Vilkårsvurdering
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.ba.sak.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest(properties = ["FAMILIE_INTEGRASJONER_API_URL=http://localhost:28085/api"])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-brev-klient", "mock-oauth", "mock-pdl", "mock-arbeidsfordeling")
@Tag("integration")
@AutoConfigureWireMock(port = 28085)
class VedtakServiceTest(
        @Autowired
        private val behandlingRepository: BehandlingRepository,

        @Autowired
        private val vedtakRepository: VedtakRepository,

        @Autowired
        private val behandlingMetrikker: BehandlingMetrikker,

        @Autowired
        private val vilkårsvurderingService: VilkårsvurderingService,

        @Autowired
        private val arbeidsfordelingService: ArbeidsfordelingService,

        @Autowired
        private val vedtakService: VedtakService,

        @Autowired
        private val persongrunnlagService: PersongrunnlagService,

        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val fagsakPersonRepository: FagsakPersonRepository,

        @Autowired
        private val totrinnskontrollService: TotrinnskontrollService,

        @Autowired
        private val loggService: LoggService,

        @Autowired
        private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,

        @Autowired
        private val oppgaveService: OppgaveService,

        @Autowired
        private val featureToggleService: FeatureToggleService,
) {

    lateinit var behandlingService: BehandlingService
    lateinit var vilkårResultat1: VilkårResultat
    lateinit var vilkårResultat2: VilkårResultat
    lateinit var vilkårResultat3: VilkårResultat
    lateinit var vilkårsvurdering: Vilkårsvurdering
    lateinit var personResultat: PersonResultat
    lateinit var vilkår: Vilkår
    lateinit var resultat: Resultat
    lateinit var behandling: Behandling

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        behandlingService = BehandlingService(
                behandlingRepository,
                behandlingMetrikker,
                fagsakPersonRepository,
                vedtakRepository,
                loggService,
                arbeidsfordelingService,
                saksstatistikkEventPublisher,
                oppgaveService,
                featureToggleService
        )

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
        resultat = Resultat.OPPFYLT

        vilkårsvurdering = lagVilkårsvurdering(personIdent, behandling, resultat)

        personResultat = PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                personIdent = personIdent
        )

        vilkårResultat1 = VilkårResultat(1, personResultat, vilkår, resultat,
                                         LocalDate.of(2010, 1, 1), LocalDate.of(2010, 6, 1),
                                         "", vilkårsvurdering.behandling.id, regelInput = null, regelOutput = null)
        vilkårResultat2 = VilkårResultat(2, personResultat, vilkår, resultat,
                                         LocalDate.of(2010, 6, 2), LocalDate.of(2010, 8, 1),
                                         "", vilkårsvurdering.behandling.id, regelInput = null, regelOutput = null)
        vilkårResultat3 = VilkårResultat(3, personResultat, vilkår, resultat,
                                         LocalDate.of(2010, 8, 2), LocalDate.of(2010, 12, 1),
                                         "", vilkårsvurdering.behandling.id, regelInput = null, regelOutput = null)
        personResultat.setSortedVilkårResultater(setOf(vilkårResultat1,
                                                       vilkårResultat2,
                                                       vilkårResultat3).toSortedSet(VilkårResultatComparator))
    }

    @Test
    @Tag("integration")
    fun `Opprett behandling med vedtak`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)

        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val vilkårsvurdering = lagVilkårsvurdering(fnr, behandling, Resultat.OPPFYLT)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling = behandling)

        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling, "ansvarligSaksbehandler", "saksbehandlerId")
        totrinnskontrollService.besluttTotrinnskontroll(behandling, "ansvarligBeslutter", "beslutterId", Beslutning.GODKJENT)

        val hentetVedtak = vedtakService.hentAktivForBehandling(behandling.id)
        Assertions.assertNotNull(hentetVedtak)
        Assertions.assertNull(hentetVedtak!!.vedtaksdato)
        Assertions.assertEquals(null, hentetVedtak.stønadBrevPdF)

        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandlingId = behandling.id)
        Assertions.assertNotNull(totrinnskontroll)
        Assertions.assertEquals("ansvarligSaksbehandler", totrinnskontroll!!.saksbehandler)
        Assertions.assertEquals("saksbehandlerId", totrinnskontroll.saksbehandlerId)
        Assertions.assertEquals("ansvarligBeslutter", totrinnskontroll.beslutter)
        Assertions.assertEquals("beslutterId", totrinnskontroll.beslutterId)
    }

    @Test
    @Tag("integration")
    fun `Legg til opplysningsplikt begrunnelse og vedtak er riktig`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)

        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val vilkårsvurdering = lagVilkårsvurdering(fnr, behandling, Resultat.IKKE_OPPFYLT)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling = behandling)

        vedtakService.leggTilVedtakBegrunnelse(
                RestPostVedtakBegrunnelse(
                        fom = LocalDate.now().minusMonths(1),
                        tom = LocalDate.now().plusYears(2),
                        vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.OPPHØR_IKKE_MOTTATT_OPPLYSNINGER
                ), fagsak.id
        )

        val hentetVedtak = vedtakService.hentAktivForBehandling(behandling.id)
        Assertions.assertTrue(hentetVedtak?.vedtakBegrunnelser?.any { vedtakBegrunnelse -> vedtakBegrunnelse.begrunnelse == VedtakBegrunnelseSpesifikasjon.OPPHØR_IKKE_MOTTATT_OPPLYSNINGER }!!)
    }

    @Test
    @Tag("integration")
    fun `Kast feil når det forsøkes å oppdatere et vedtak som ikke er lagret`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        assertThrows<IllegalStateException> {
            vedtakService.oppdater(Vedtak(behandling = behandling,
                                          vedtaksdato = LocalDateTime.now())
            )
        }
    }

    @Test
    fun `Opphørsdato settes til neste måned på vedtak dersom behandlingresultat er opphørt`() {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak).also { it.resultat = BehandlingResultat.OPPHØRT })
        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling = behandling)
        vedtakService.oppdaterOpphørsdatoForOppdragPåVedtak(behandlingId = behandling.id)
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)

        Assertions.assertEquals(LocalDate.now().førsteDagINesteMåned(), vedtak!!.opphørsdatoForOppdrag)
    }

    @Test
    fun `Opphørsdato settes ikke på vedtak dersom behandlingresultat ikke er opphørt`() {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak).also {
            it.resultat = BehandlingResultat.INNVILGET_OG_OPPHØRT
        })
        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling = behandling)
        vedtakService.oppdaterOpphørsdatoForOppdragPåVedtak(behandlingId = behandling.id)
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)

        Assertions.assertEquals(null, vedtak!!.opphørsdatoForOppdrag)
    }
}