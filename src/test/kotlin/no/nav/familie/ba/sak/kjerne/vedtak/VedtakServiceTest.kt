package no.nav.familie.ba.sak.kjerne.vedtak

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.mockk.MockKAnnotations
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestPostVedtakBegrunnelse
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingMetrikker
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakPersonRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat.Companion.VilkårResultatComparator
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

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
        private val vedtaksperiodeService: VedtaksperiodeService,

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
        private val infotrygdService: InfotrygdService,

        @Autowired
        private val featureToggleService: FeatureToggleService,

        @Autowired
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,

        @Autowired
        private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) : AbstractSpringIntegrationTest() {

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
                personopplysningGrunnlagRepository,
                andelTilkjentYtelseRepository,
                behandlingMetrikker,
                fagsakPersonRepository,
                vedtakRepository,
                loggService,
                arbeidsfordelingService,
                saksstatistikkEventPublisher,
                oppgaveService,
                infotrygdService,
                vedtaksperiodeService,
                featureToggleService
        )

        stubFor(
                get(urlEqualTo("/api/aktoer/v1"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(objectMapper.writeValueAsString(Ressurs.success(mapOf("aktørId" to "1"))))
                        )
        )

        val personIdent = randomFnr()

        behandling = lagBehandling()

        vilkår = Vilkår.LOVLIG_OPPHOLD
        resultat = Resultat.OPPFYLT

        vilkårsvurdering = lagVilkårsvurdering(personIdent, behandling, resultat)

        personResultat = PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                personIdent = personIdent
        )

        vilkårResultat1 = VilkårResultat(
                1, personResultat, vilkår, resultat,
                LocalDate.of(2010, 1, 1), LocalDate.of(2010, 6, 1),
                "", vilkårsvurdering.behandling.id
        )
        vilkårResultat2 = VilkårResultat(
                2, personResultat, vilkår, resultat,
                LocalDate.of(2010, 6, 2), LocalDate.of(2010, 8, 1),
                "", vilkårsvurdering.behandling.id,
        )
        vilkårResultat3 = VilkårResultat(
                3, personResultat, vilkår, resultat,
                LocalDate.of(2010, 8, 2), LocalDate.of(2010, 12, 1),
                "", vilkårsvurdering.behandling.id
        )
        personResultat.setSortedVilkårResultater(
                setOf(
                        vilkårResultat1,
                        vilkårResultat2,
                        vilkårResultat3
                ).toSortedSet(VilkårResultatComparator)
        )
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
            vedtakService.oppdater(
                    Vedtak(
                            behandling = behandling,
                            vedtaksdato = LocalDateTime.now()
                    )
            )
        }
    }
}