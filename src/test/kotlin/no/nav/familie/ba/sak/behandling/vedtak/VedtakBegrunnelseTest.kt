package no.nav.familie.ba.sak.behandling.vedtak

import io.mockk.MockKAnnotations
import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.BehandlingMetrikker
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakPersonRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.RestDeleteVedtakBegrunnelser
import no.nav.familie.ba.sak.behandling.restDomene.RestPostFritekstVedtakBegrunnelser
import no.nav.familie.ba.sak.behandling.restDomene.RestPostVedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseType
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkårsvurdering
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVedtakBegrunnesle
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.ba.sak.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.ba.sak.tilbakekreving.TilbakekrevingService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate

@SpringBootTest
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles(
        "mock-pdl",
        "postgres",
        "mock-arbeidsfordeling",
        "mock-infotrygd-barnetrygd",
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VedtakBegrunnelseTest(
        @Autowired
        private val behandlingRepository: BehandlingRepository,

        @Autowired
        private val vedtakRepository: VedtakRepository,

        @Autowired
        private val behandlingMetrikker: BehandlingMetrikker,

        @Autowired
        private val vilkårsvurderingService: VilkårsvurderingService,

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
        private val loggService: LoggService,

        @Autowired
        private val arbeidsfordelingService: ArbeidsfordelingService,

        @Autowired
        private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,

        @Autowired
        private val oppgaveService: OppgaveService,

        @Autowired
        private val stegService: StegService,

        @Autowired
        private val tilbakekrevingService: TilbakekrevingService,

        @Autowired
        private val infotrygdService: InfotrygdService,
) {

    lateinit var behandlingService: BehandlingService

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
                infotrygdService,
                vedtaksperiodeService
        )
    }

    @Test
    fun `Lagring av innvilgelsesbegrunnelser skal koble seg til korrekt vilkår`() {
        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barn1Fnr, barn2Fnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = Vilkårsvurdering(
                behandling = behandling
        )

        val søkerPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = søkerFnr)
        søkerPersonResultat.setSortedVilkårResultater(setOf(
                VilkårResultat(
                        personResultat = søkerPersonResultat,
                        vilkårType = Vilkår.LOVLIG_OPPHOLD,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = LocalDate.of(2009, 12, 24),
                        periodeTom = LocalDate.of(2010, 6, 1),
                        begrunnelse = "",
                        behandlingId = vilkårsvurdering.behandling.id,
                        regelInput = null,
                        regelOutput = null),
                VilkårResultat(
                        personResultat = søkerPersonResultat,
                        vilkårType = Vilkår.BOSATT_I_RIKET,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = LocalDate.of(2009, 12, 24),
                        periodeTom = LocalDate.of(2010, 6, 1),
                        begrunnelse = "",
                        behandlingId = vilkårsvurdering.behandling.id,
                        regelInput = null,
                        regelOutput = null)))

        val barn1PersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barn1Fnr)

        barn1PersonResultat.setSortedVilkårResultater(setOf(
                VilkårResultat(personResultat = barn1PersonResultat,
                               vilkårType = Vilkår.LOVLIG_OPPHOLD,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = LocalDate.of(2009, 12, 24),
                               periodeTom = LocalDate.of(2010, 6, 1),
                               begrunnelse = "",
                               behandlingId = vilkårsvurdering.behandling.id,
                               regelInput = null,
                               regelOutput = null),
                VilkårResultat(personResultat = barn1PersonResultat,
                               vilkårType = Vilkår.GIFT_PARTNERSKAP,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = LocalDate.of(2009, 11, 24),
                               periodeTom = LocalDate.of(2010, 6, 1),
                               begrunnelse = "",
                               behandlingId = vilkårsvurdering.behandling.id,
                               regelInput = null,
                               regelOutput = null)))

        val barn2PersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barn1Fnr)

        barn2PersonResultat.setSortedVilkårResultater(setOf(
                VilkårResultat(personResultat = barn1PersonResultat,
                               vilkårType = Vilkår.LOVLIG_OPPHOLD,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = LocalDate.of(2010, 2, 24),
                               periodeTom = LocalDate.of(2010, 6, 1),
                               begrunnelse = "",
                               behandlingId = vilkårsvurdering.behandling.id,
                               regelInput = null,
                               regelOutput = null),
                VilkårResultat(personResultat = barn1PersonResultat,
                               vilkårType = Vilkår.GIFT_PARTNERSKAP,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = LocalDate.of(2009, 11, 24),
                               periodeTom = LocalDate.of(2010, 6, 1),
                               begrunnelse = "",
                               behandlingId = vilkårsvurdering.behandling.id,
                               regelInput = null,
                               regelOutput = null)))

        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barn1PersonResultat, barn2PersonResultat)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering)

        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling)

        val begrunnelserLovligOpphold =
                vedtakService.leggTilVedtakBegrunnelse(restPostVedtakBegrunnelse = RestPostVedtakBegrunnelse(
                        fom = LocalDate.of(2010, 1, 1),
                        tom = LocalDate.of(2010, 6, 1),
                        vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE
                ), fagsakId = fagsak.id)

        assert(begrunnelserLovligOpphold.size == 1)
        assertEquals(
                "Du får barnetrygd fordi du og barn født 01.01.19 har oppholdstillatelse fra desember 2009.",
                begrunnelserLovligOpphold.firstOrNull { it.begrunnelse == VedtakBegrunnelseSpesifikasjon.INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE }!!.brevBegrunnelse)

        val begrunnelserLovligOppholdOgBosattIRiket =
                vedtakService.leggTilVedtakBegrunnelse(restPostVedtakBegrunnelse = RestPostVedtakBegrunnelse(
                        fom = LocalDate.of(2010, 1, 1),
                        tom = LocalDate.of(2010, 6, 1),
                        vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET
                ), fagsakId = fagsak.id)

        assert(begrunnelserLovligOppholdOgBosattIRiket.size == 2)
        assertEquals(
                "Du får barnetrygd fordi du er bosatt i Norge fra desember 2009.",
                begrunnelserLovligOppholdOgBosattIRiket.firstOrNull { it.begrunnelse == VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET }!!.brevBegrunnelse)
    }

    @Test
    fun `Lagring av reduksjonsbegrunnelse grunnet fylte 18 år skal genere riktig brevtekst`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id,
                                                søkerFnr,
                                                listOf(barnFnr),
                                                barnFødselsdato = LocalDate.of(2010, 12, 24))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = Vilkårsvurdering(
                behandling = behandling
        )

        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barnFnr)

        barnPersonResultat.setSortedVilkårResultater(setOf(
                VilkårResultat(personResultat = barnPersonResultat,
                               vilkårType = Vilkår.UNDER_18_ÅR,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = LocalDate.of(2010, 12, 24),
                               periodeTom = LocalDate.of(2028, 12, 24),
                               begrunnelse = "",
                               behandlingId = vilkårsvurdering.behandling.id,
                               regelInput = null,
                               regelOutput = null)))


        vilkårsvurdering.personResultater = setOf(barnPersonResultat)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering)

        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling)

        val begrunnelser18år =
                vedtakService.leggTilVedtakBegrunnelse(restPostVedtakBegrunnelse = RestPostVedtakBegrunnelse(
                        fom = LocalDate.of(2028, 12, 1),
                        tom = LocalDate.of(2035, 6, 30),
                        vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_18_ÅR
                ), fagsakId = fagsak.id)

        assert(begrunnelser18år.size == 1)
        assertEquals(
                "Barnetrygden reduseres fordi barn født 24.12.10 fylte 18 år.",
                begrunnelser18år.firstOrNull { it.begrunnelse == VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_18_ÅR }!!.brevBegrunnelse)

    }

    @Test
    fun `Lagring av reduksjonsbegrunnelse med grenseverdi på tom på vilkår`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id,
                                                søkerFnr,
                                                listOf(barnFnr),
                                                barnFødselsdato = LocalDate.of(2010, 12, 24))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = Vilkårsvurdering(
                behandling = behandling
        )

        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barnFnr)

        barnPersonResultat.setSortedVilkårResultater(setOf(
                VilkårResultat(personResultat = barnPersonResultat,
                               vilkårType = Vilkår.BOSATT_I_RIKET,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = LocalDate.of(2010, 12, 24),
                               periodeTom = LocalDate.of(2021, 3, 31),
                               begrunnelse = "",
                               behandlingId = vilkårsvurdering.behandling.id,
                               regelInput = null,
                               regelOutput = null)))


        vilkårsvurdering.personResultater = setOf(barnPersonResultat)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering)

        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling)

        val begrunnelserBosattIRiket =
                vedtakService.leggTilVedtakBegrunnelse(restPostVedtakBegrunnelse = RestPostVedtakBegrunnelse(
                        fom = LocalDate.of(2021, 4, 1),
                        tom = null,
                        vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.REDUKSJON_BOSATT_I_RIKTET
                ), fagsakId = fagsak.id)

        assert(begrunnelserBosattIRiket.size == 1)
        assertEquals(
                "Barnetrygden reduseres fordi barn født 24.12.10 har flyttet fra Norge i mars 2021.",
                begrunnelserBosattIRiket.firstOrNull { it.begrunnelse == VedtakBegrunnelseSpesifikasjon.REDUKSJON_BOSATT_I_RIKTET }!!.brevBegrunnelse)

    }

    @Test
    fun `Lagring av opphørsbegrunnelse skal generere riktig brevtekst`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()

        val behandlingEtterRegistrerSøknadSteg = kjørStegprosessForFGB(
                tilSteg = StegType.REGISTRERE_SØKNAD,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(barnFnr),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                tilbakekrevingService = tilbakekrevingService
        )

        val vilkårsvurdering = Vilkårsvurdering(
                behandling = behandlingEtterRegistrerSøknadSteg
        )

        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barnFnr)

        val innvilgetVilkårsvurderingPåBarnTom = inneværendeMåned().minusMonths(2)
        barnPersonResultat.setSortedVilkårResultater(setOf(
                VilkårResultat(personResultat = barnPersonResultat,
                               vilkårType = Vilkår.BOSATT_I_RIKET,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = inneværendeMåned().minusYears(1).toLocalDate(),
                               periodeTom = innvilgetVilkårsvurderingPåBarnTom.toLocalDate(),
                               begrunnelse = "",
                               behandlingId = vilkårsvurdering.behandling.id,
                               regelInput = null,
                               regelOutput = null)))


        vilkårsvurdering.personResultater = setOf(barnPersonResultat)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering)

        val opphørsperiodeFom = innvilgetVilkårsvurderingPåBarnTom.nesteMåned()
        val begrunnelser =
                vedtakService.leggTilVedtakBegrunnelse(restPostVedtakBegrunnelse = RestPostVedtakBegrunnelse(
                        fom = opphørsperiodeFom.førsteDagIInneværendeMåned(),
                        tom = null,
                        vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.OPPHØR_BARN_UTVANDRET
                ), fagsakId = behandlingEtterRegistrerSøknadSteg.fagsak.id)

        assert(begrunnelser.size == 1)
        assertEquals(
                "Barn født ${ClientMocks.personInfo[ClientMocks.INTEGRASJONER_FNR]?.fødselsdato?.tilKortString()} har flyttet fra Norge i ${
                    innvilgetVilkårsvurderingPåBarnTom
                            .tilMånedÅr()
                }.",
                begrunnelser.firstOrNull { it.begrunnelse == VedtakBegrunnelseSpesifikasjon.OPPHØR_BARN_UTVANDRET }!!.brevBegrunnelse)

    }

    @Test
    fun `Lagring av reduksjonsbegrunnelse grunnet fylte 6 år skal genere riktig brevtekst`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id,
                                                søkerFnr,
                                                listOf(barnFnr),
                                                barnFødselsdato = LocalDate.of(2010, 12, 24))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = Vilkårsvurdering(
                behandling = behandling
        )

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering)

        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling)

        val begrunnelser6år =
                vedtakService.leggTilVedtakBegrunnelse(restPostVedtakBegrunnelse = RestPostVedtakBegrunnelse(
                        fom = LocalDate.of(2016, 12, 24),
                        tom = LocalDate.of(2035, 6, 30),
                        vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR
                ), fagsakId = fagsak.id)

        assert(begrunnelser6år.size == 1)
        assertEquals(
                "Barnetrygden reduseres fordi barn født 24.12.10 fyller 6 år.",
                begrunnelser6år.firstOrNull { it.begrunnelse == VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR }!!.brevBegrunnelse)

    }

    @Test
    fun `Skal slette alle begrunnelser for en periode på vedtaksbegrunnelsetyper`() {
        val behandlingEtterVilkårsvurderingSteg = kjørStegprosessForFGB(
                tilSteg = StegType.VILKÅRSVURDERING,
                søkerFnr = randomFnr(),
                barnasIdenter = listOf(randomFnr()),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                tilbakekrevingService = tilbakekrevingService
        )

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandlingEtterVilkårsvurderingSteg.id)
                     ?: error("Finner ikke vedtak i test")

        val førstePeriode = Periode(fom = LocalDate.of(2020, 12, 1),
                                    tom = LocalDate.of(2028, 6, 30))
        val andrePeriode = Periode(fom = LocalDate.of(2028, 12, 1),
                                   tom = LocalDate.of(2035, 6, 30))
        vedtak.leggTilBegrunnelse(VedtakBegrunnelse(
                vedtak = vedtak,
                fom = førstePeriode.fom,
                tom = førstePeriode.tom,
                begrunnelse = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET
        ))
        vedtak.leggTilBegrunnelse(VedtakBegrunnelse(
                vedtak = vedtak,
                fom = andrePeriode.fom,
                tom = andrePeriode.tom,
                begrunnelse = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOR_HOS_SØKER
        ))
        vedtak.leggTilBegrunnelse(VedtakBegrunnelse(
                vedtak = vedtak,
                fom = andrePeriode.fom,
                tom = andrePeriode.tom,
                begrunnelse = VedtakBegrunnelseSpesifikasjon.INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER
        ))
        vedtak.leggTilBegrunnelse(VedtakBegrunnelse(
                vedtak = vedtak,
                fom = andrePeriode.fom,
                tom = andrePeriode.tom,
                begrunnelse = VedtakBegrunnelseSpesifikasjon.OPPHØR_BARN_DØD
        ))
        val oppdatertVedtakMed2BegrunnelserForAndrePeriode = vedtakService.oppdater(vedtak)
        assertEquals(3,
                     oppdatertVedtakMed2BegrunnelserForAndrePeriode.vedtakBegrunnelser.filter { it.fom == andrePeriode.fom && it.tom == andrePeriode.tom }.size)

        oppdatertVedtakMed2BegrunnelserForAndrePeriode.slettBegrunnelserForPeriodeOgVedtaksbegrunnelseTyper(
                RestDeleteVedtakBegrunnelser(
                        fom = andrePeriode.fom,
                        tom = andrePeriode.tom,
                        vedtakbegrunnelseTyper = listOf(VedtakBegrunnelseType.INNVILGELSE, VedtakBegrunnelseType.REDUKSJON)
                ))
        val oppdatertVedtakUtenBegrunnelserForAndrePeriode =
                vedtakService.oppdater(oppdatertVedtakMed2BegrunnelserForAndrePeriode)
        assertEquals(1,
                     oppdatertVedtakUtenBegrunnelserForAndrePeriode.vedtakBegrunnelser.filter { it.fom == andrePeriode.fom && it.tom == andrePeriode.tom }.size)
    }

    @Test
    fun `Skal kaste feil når man velger begrunnelser som ikke passer med vilkårsvurderingen`() {
        val behandlingEtterVilkårsvurderingSteg = kjørStegprosessForFGB(
                tilSteg = StegType.VILKÅRSVURDERING,
                søkerFnr = randomFnr(),
                barnasIdenter = listOf(randomFnr()),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                tilbakekrevingService = tilbakekrevingService
        )

        val innvilgetFeil = assertThrows<FunksjonellFeil> {
            vedtakService.leggTilVedtakBegrunnelse(restPostVedtakBegrunnelse = RestPostVedtakBegrunnelse(
                    fom = LocalDate.of(2020, 1, 1),
                    tom = LocalDate.of(2020, 6, 1),
                    vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE
            ), fagsakId = behandlingEtterVilkårsvurderingSteg.fagsak.id)
        }

        assertEquals("Begrunnelsen samsvarte ikke med vilkårsvurderingen", innvilgetFeil.message)

        val reduksjonFeil = assertThrows<FunksjonellFeil> {
            vedtakService.leggTilVedtakBegrunnelse(restPostVedtakBegrunnelse = RestPostVedtakBegrunnelse(
                    fom = LocalDate.of(2020, 1, 1),
                    tom = LocalDate.of(2020, 6, 1),
                    vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.REDUKSJON_BOSATT_I_RIKTET
            ), fagsakId = behandlingEtterVilkårsvurderingSteg.fagsak.id)
        }

        assertEquals("Begrunnelsen samsvarte ikke med vilkårsvurderingen", reduksjonFeil.message)

        val satsendringFeil = assertThrows<FunksjonellFeil> {
            vedtakService.leggTilVedtakBegrunnelse(restPostVedtakBegrunnelse = RestPostVedtakBegrunnelse(
                    fom = LocalDate.of(2020, 1, 1),
                    tom = LocalDate.of(2020, 6, 1),
                    vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.INNVILGET_SATSENDRING
            ), fagsakId = behandlingEtterVilkårsvurderingSteg.fagsak.id)
        }

        assertEquals("Begrunnelsen stemmer ikke med satsendring.", satsendringFeil.message)
    }


    @Test
    fun `Legg til fritekster til vedtakbegrunnelser`() {
        val fritekst1 = "fritekst1";
        val fritekst2 = "fritekst2";

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling)
        val vedtak = vedtakService.settFritekstbegrunnelserPåVedtaksperiodeOgType(
                fagsakId = fagsak.id,
                restPostFritekstVedtakBegrunnelser = RestPostFritekstVedtakBegrunnelser(
                        fom = LocalDate.now().minusMonths(1),
                        tom = LocalDate.now(),
                        fritekster = listOf(fritekst1, fritekst2),
                        vedtaksperiodetype = Vedtaksperiodetype.OPPHØR),
                validerKombinasjoner = false)

        Assertions.assertTrue(vedtak.vedtakBegrunnelser.any { it.brevBegrunnelse == fritekst1 })
        Assertions.assertTrue(vedtak.vedtakBegrunnelser.any { it.brevBegrunnelse == fritekst2 })
    }

    @Test
    fun `Sjekk at gamle fritekster blir overskrevet når nye blir lagt til vedtakbegrunnelser`() {
        val fritekst1 = "fritekst1";
        val fritekst2 = "fritekst2";
        val fritekst3 = "fritekst3";
        val fritekst4 = "fritekst4";

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling)
        vedtakService.settFritekstbegrunnelserPåVedtaksperiodeOgType(
                fagsakId = fagsak.id,
                restPostFritekstVedtakBegrunnelser = RestPostFritekstVedtakBegrunnelser(fom = LocalDate.now().minusMonths(1),
                                                                                        tom = LocalDate.now(),
                                                                                        fritekster = listOf(fritekst1, fritekst2),
                                                                                        vedtaksperiodetype = Vedtaksperiodetype.OPPHØR),
                validerKombinasjoner = false)

        val vedtak = vedtakService.settFritekstbegrunnelserPåVedtaksperiodeOgType(
                fagsakId = fagsak.id,
                restPostFritekstVedtakBegrunnelser = RestPostFritekstVedtakBegrunnelser(fom = LocalDate.now().minusMonths(1),
                                                                                        tom = LocalDate.now(),
                                                                                        fritekster = listOf(fritekst3, fritekst4),
                                                                                        vedtaksperiodetype = Vedtaksperiodetype.OPPHØR),
                validerKombinasjoner = false)

        Assertions.assertFalse(vedtak.vedtakBegrunnelser.any { it.brevBegrunnelse == fritekst1 })
        Assertions.assertFalse(vedtak.vedtakBegrunnelser.any { it.brevBegrunnelse == fritekst2 })
        Assertions.assertTrue(vedtak.vedtakBegrunnelser.any { it.brevBegrunnelse == fritekst3 })
        Assertions.assertTrue(vedtak.vedtakBegrunnelser.any { it.brevBegrunnelse == fritekst4 })
    }

    @Test
    fun `valider opphør fritekst trenger begrunnelse av tilsvarende type og for samme periode`() {
        val fritekst1 = "fritekst1";
        val fritekst2 = "fritekst2";

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling)

        Assertions.assertThrows(FunksjonellFeil::class.java) {
            vedtakService.settFritekstbegrunnelserPåVedtaksperiodeOgType(
                    fagsakId = fagsak.id,
                    restPostFritekstVedtakBegrunnelser = RestPostFritekstVedtakBegrunnelser(fom = LocalDate.now().minusMonths(1),
                                                                                            tom = LocalDate.now(),
                                                                                            fritekster = listOf(fritekst1,
                                                                                                                fritekst2),
                                                                                            vedtaksperiodetype = Vedtaksperiodetype.OPPHØR))
        }
    }

    @Test
    fun `valider reduksjon fritekst trenger begrunnelse av tilsvarende type og for samme periode`() {
        val fritekst1 = "fritekst1";
        val fritekst2 = "fritekst2";

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling)

        Assertions.assertThrows(FunksjonellFeil::class.java) {
            vedtakService.settFritekstbegrunnelserPåVedtaksperiodeOgType(
                    fagsakId = fagsak.id,
                    restPostFritekstVedtakBegrunnelser = RestPostFritekstVedtakBegrunnelser(fom = LocalDate.now().minusMonths(1),
                                                                                            tom = LocalDate.now().minusMonths(1),
                                                                                            fritekster = listOf(fritekst1,
                                                                                                                fritekst2),
                                                                                            vedtaksperiodetype = Vedtaksperiodetype.UTBETALING))
        }
    }

    @Test
    fun `valider avslag fritekst trenger ikke begrunnelse av tilsvarende type`() {
        val fritekst1 = "fritekst1";
        val fritekst2 = "fritekst2";

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling)
        val vedtak = vedtakService.settFritekstbegrunnelserPåVedtaksperiodeOgType(
                fagsakId = fagsak.id,
                restPostFritekstVedtakBegrunnelser = RestPostFritekstVedtakBegrunnelser(fom = LocalDate.now().minusMonths(1),
                                                                                        tom = LocalDate.now(),
                                                                                        fritekster = listOf(fritekst1, fritekst2),
                                                                                        vedtaksperiodetype = Vedtaksperiodetype.AVSLAG))

        Assertions.assertTrue(vedtak.validerVedtakBegrunnelserForFritekstOpphørOgReduksjon())
    }

    @Test
    fun `valider opphør fritekst med begrunnelse av tilsvarende type og for samme periode validerer`() {
        val fritekst1 = "fritekst1";
        val fritekst2 = "fritekst2";

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling)
        val vedtak = vedtakService.settFritekstbegrunnelserPåVedtaksperiodeOgType(
                fagsakId = fagsak.id,
                restPostFritekstVedtakBegrunnelser = RestPostFritekstVedtakBegrunnelser(fom = LocalDate.now().minusMonths(1),
                                                                                        tom = LocalDate.now(),
                                                                                        fritekster = listOf(fritekst1, fritekst2),
                                                                                        vedtaksperiodetype = Vedtaksperiodetype.OPPHØR),
                validerKombinasjoner = false)

        vedtak.leggTilBegrunnelse(lagVedtakBegrunnesle(fom = LocalDate.now().minusMonths(1),
                                                       tom = LocalDate.now(),
                                                       vedtak = vedtak,
                                                       vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.OPPHØR_BARN_FLYTTET_FRA_SØKER))

        Assertions.assertTrue(vedtak.validerVedtakBegrunnelserForFritekstOpphørOgReduksjon())
    }
}