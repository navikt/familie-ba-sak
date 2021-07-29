package no.nav.familie.ba.sak.kjerne.vedtak

import io.mockk.MockKAnnotations
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
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.ekstern.restDomene.RestDeleteVedtakBegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestPostFritekstVedtakBegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestPostVedtakBegrunnelse
import no.nav.familie.ba.sak.ekstern.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingMetrikker
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakPersonRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon.Companion.finnVilkårFor
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon.Companion.tilBrevTekst
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseUtils.vedtakBegrunnelserIkkeTilknyttetVilkår
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime


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
        private val vilkårService: VilkårService,

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

        @Autowired
        private val featureToggleService: FeatureToggleService,

        @Autowired
        private val databaseCleanupService: DatabaseCleanupService
) : AbstractSpringIntegrationTest() {

    lateinit var behandlingService: BehandlingService

    @BeforeEach
    fun setup() {
        databaseCleanupService.truncate()
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
                vedtaksperiodeService,
                featureToggleService
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
                        behandlingId = vilkårsvurdering.behandling.id),
                VilkårResultat(
                        personResultat = søkerPersonResultat,
                        vilkårType = Vilkår.BOSATT_I_RIKET,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = LocalDate.of(2009, 12, 24),
                        periodeTom = LocalDate.of(2010, 6, 1),
                        begrunnelse = "",
                        behandlingId = vilkårsvurdering.behandling.id)))

        val barn1PersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barn1Fnr)

        barn1PersonResultat.setSortedVilkårResultater(setOf(
                VilkårResultat(personResultat = barn1PersonResultat,
                               vilkårType = Vilkår.LOVLIG_OPPHOLD,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = LocalDate.of(2009, 12, 24),
                               periodeTom = LocalDate.of(2010, 6, 1),
                               begrunnelse = "",
                               behandlingId = vilkårsvurdering.behandling.id),
                VilkårResultat(personResultat = barn1PersonResultat,
                               vilkårType = Vilkår.GIFT_PARTNERSKAP,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = LocalDate.of(2009, 11, 24),
                               periodeTom = LocalDate.of(2010, 6, 1),
                               begrunnelse = "",
                               behandlingId = vilkårsvurdering.behandling.id)))

        val barn2PersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barn1Fnr)

        barn2PersonResultat.setSortedVilkårResultater(setOf(
                VilkårResultat(personResultat = barn1PersonResultat,
                               vilkårType = Vilkår.LOVLIG_OPPHOLD,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = LocalDate.of(2010, 2, 24),
                               periodeTom = LocalDate.of(2010, 6, 1),
                               begrunnelse = "",
                               behandlingId = vilkårsvurdering.behandling.id),
                VilkårResultat(personResultat = barn1PersonResultat,
                               vilkårType = Vilkår.GIFT_PARTNERSKAP,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = LocalDate.of(2009, 11, 24),
                               periodeTom = LocalDate.of(2010, 6, 1),
                               begrunnelse = "",
                               behandlingId = vilkårsvurdering.behandling.id)))

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
                "Du får barnetrygd for barn født 01.01.19 fordi du og barnet har oppholdstillatelse fra desember 2009.",
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
        val fødselsdato = LocalDate.now().minusYears(18).førsteDagIInneværendeMåned().plusDays(24)
        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id,
                                                søkerFnr,
                                                listOf(barnFnr),
                                                barnFødselsdato = fødselsdato)
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = Vilkårsvurdering(
                behandling = behandling
        )

        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barnFnr)

        barnPersonResultat.setSortedVilkårResultater(setOf(
                VilkårResultat(personResultat = barnPersonResultat,
                               vilkårType = Vilkår.UNDER_18_ÅR,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = fødselsdato,
                               periodeTom = fødselsdato.plusYears(18),
                               begrunnelse = "",
                               behandlingId = vilkårsvurdering.behandling.id)))


        vilkårsvurdering.personResultater = setOf(barnPersonResultat)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering)

        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling)

        val begrunnelser18år =
                vedtakService.leggTilVedtakBegrunnelse(restPostVedtakBegrunnelse = RestPostVedtakBegrunnelse(
                        fom = fødselsdato.plusYears(18).førsteDagIInneværendeMåned(),
                        tom = LocalDate.of(2035, 6, 30),
                        vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_18_ÅR
                ), fagsakId = fagsak.id)

        assert(begrunnelser18år.size == 1)
        val datoerIBrev = listOf(fødselsdato).tilBrevTekst()
        assertEquals(
                "Barnetrygden reduseres fordi barn født $datoerIBrev er 18 år.",
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
                               behandlingId = vilkårsvurdering.behandling.id)))


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
                tilbakekrevingService = tilbakekrevingService,
                vedtaksperiodeService = vedtaksperiodeService,
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
                               behandlingId = vilkårsvurdering.behandling.id)))


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
        val fødselsdato = LocalDate.now().minusYears(6).førsteDagIInneværendeMåned().plusDays(24)
        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id,
                                                søkerFnr,
                                                listOf(barnFnr),
                                                barnFødselsdato = fødselsdato)
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = Vilkårsvurdering(
                behandling = behandling
        )

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering)

        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling)

        val begrunnelser6år =
                vedtakService.leggTilVedtakBegrunnelse(restPostVedtakBegrunnelse = RestPostVedtakBegrunnelse(
                        fom = fødselsdato.plusYears(6).førsteDagIInneværendeMåned(),
                        tom = LocalDate.of(2035, 6, 30),
                        vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR
                ), fagsakId = fagsak.id)

        assert(begrunnelser6år.size == 1)
        val datoerIBrev = listOf(fødselsdato).tilBrevTekst()
        assertEquals(
                "Barnetrygden reduseres fordi barn født $datoerIBrev er 6 år.",
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
                tilbakekrevingService = tilbakekrevingService,
                vedtaksperiodeService = vedtaksperiodeService,
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
                tilbakekrevingService = tilbakekrevingService,
                vedtaksperiodeService = vedtaksperiodeService,
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
    fun `Skal sjekke at kun bor med søker begrunnelser er valgbare`() {
        val behandlingEtterVilkårsvurderingSteg = kjørStegprosessForFGB(
                tilSteg = StegType.VILKÅRSVURDERING,
                søkerFnr = randomFnr(),
                barnasIdenter = listOf(ClientMocks.barnFnr[0]),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                tilbakekrevingService = tilbakekrevingService,
                vedtaksperiodeService = vedtaksperiodeService,
        )
        val vilkårsvurdering =
                vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandlingEtterVilkårsvurderingSteg.id)!!
        val vilkårUtenomBorMedSøkerForBarn =
                vilkårsvurdering.personResultater.find { it.personIdent == ClientMocks.barnFnr[0] }!!.vilkårResultater.filter { it.vilkårType != Vilkår.BOR_MED_SØKER && it.vilkårType != Vilkår.UNDER_18_ÅR }

        vilkårUtenomBorMedSøkerForBarn.forEach {
            vilkårService.endreVilkår(behandlingId = behandlingEtterVilkårsvurderingSteg.id,
                                      vilkårId = it.id,
                                      restPersonResultat = RestPersonResultat(
                                              personIdent = ClientMocks.barnFnr[0],
                                              vilkårResultater = listOf(RestVilkårResultat(
                                                      periodeFom = LocalDate.now().minusMonths(2),
                                                      periodeTom = null,
                                                      resultat = Resultat.OPPFYLT,
                                                      begrunnelse = "",
                                                      behandlingId = behandlingEtterVilkårsvurderingSteg.id,
                                                      endretAv = "",
                                                      endretTidspunkt = LocalDateTime.now(),
                                                      id = it.id,
                                                      vilkårType = it.vilkårType
                                              ))
                                      ))
        }


        val behandlingEtterVilkårsvurderingStegGang2 =
                stegService.håndterVilkårsvurdering(behandlingEtterVilkårsvurderingSteg)

        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandlingEtterVilkårsvurderingStegGang2.id)

        val restVedtaksperioderMedBegrunnelser = vedtaksperiodeService.hentRestVedtaksperiodeMedBegrunnelser(vedtak)

        restVedtaksperioderMedBegrunnelser.forEach {
            it.begrunnelser.forEach { restVedtaksbegrunnelse ->
                if (restVedtaksbegrunnelse.vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGELSE) {
                    assertEquals(Vilkår.BOR_MED_SØKER,
                                 restVedtaksbegrunnelse.vedtakBegrunnelseSpesifikasjon.finnVilkårFor())
                } else {
                    assertTrue(vedtakBegrunnelserIkkeTilknyttetVilkår.contains(restVedtaksbegrunnelse.vedtakBegrunnelseSpesifikasjon))
                }
            }
        }
        assertEquals(1, restVedtaksperioderMedBegrunnelser.size)
    }

    @Test
    fun `Skal sjekke at kun opphørsbegrunnelser er valgbare ved opphør`() {
        val behandlingEtterVilkårsvurderingSteg = kjørStegprosessForFGB(
                tilSteg = StegType.VILKÅRSVURDERING,
                søkerFnr = randomFnr(),
                barnasIdenter = listOf(ClientMocks.barnFnr[0]),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                tilbakekrevingService = tilbakekrevingService,
                vedtaksperiodeService = vedtaksperiodeService,
        )
        val vilkårsvurdering =
                vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandlingEtterVilkårsvurderingSteg.id)!!
        vilkårsvurdering.personResultater.forEach { personResultat ->
            personResultat.vilkårResultater.filter { it.vilkårType != Vilkår.UNDER_18_ÅR }
                    .forEach { vilkårUtenomUnder18År ->
                        vilkårService.endreVilkår(behandlingId = behandlingEtterVilkårsvurderingSteg.id,
                                                  vilkårId = vilkårUtenomUnder18År.id,
                                                  restPersonResultat = RestPersonResultat(
                                                          personIdent = personResultat.personIdent,
                                                          vilkårResultater = listOf(RestVilkårResultat(
                                                                  periodeFom = LocalDate.now().minusMonths(4),
                                                                  periodeTom = LocalDate.now().minusMonths(1),
                                                                  resultat = Resultat.OPPFYLT,
                                                                  begrunnelse = "",
                                                                  behandlingId = behandlingEtterVilkårsvurderingSteg.id,
                                                                  endretAv = "",
                                                                  endretTidspunkt = LocalDateTime.now(),
                                                                  id = vilkårUtenomUnder18År.id,
                                                                  vilkårType = vilkårUtenomUnder18År.vilkårType
                                                          ))
                                                  ))
                    }
        }


        val behandlingEtterVilkårsvurderingStegGang2 =
                stegService.håndterVilkårsvurdering(behandlingEtterVilkårsvurderingSteg)

        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandlingEtterVilkårsvurderingStegGang2.id)

        val restVedtaksperioderMedBegrunnelser = vedtaksperiodeService.hentRestVedtaksperiodeMedBegrunnelser(vedtak)

        assertEquals(1, restVedtaksperioderMedBegrunnelser.filter { it.type == Vedtaksperiodetype.UTBETALING }.size)
        assertEquals(1, restVedtaksperioderMedBegrunnelser.filter { it.type == Vedtaksperiodetype.OPPHØR }.size)
        assertEquals(0,
                     restVedtaksperioderMedBegrunnelser.filter { it.type != Vedtaksperiodetype.UTBETALING && it.type != Vedtaksperiodetype.OPPHØR }.size)

        val gyldigeOpphørsbegrunnelser = VedtakBegrunnelseSpesifikasjon.values()
                .filter { it.vedtakBegrunnelseType == VedtakBegrunnelseType.OPPHØR && it != VedtakBegrunnelseSpesifikasjon.OPPHØR_UNDER_18_ÅR && !it.erFritekstBegrunnelse() && it.erTilgjengeligFrontend }

        assertEquals(gyldigeOpphørsbegrunnelser.size,
                     restVedtaksperioderMedBegrunnelser.find { it.type == Vedtaksperiodetype.OPPHØR }?.gyldigeBegrunnelser?.size)
    }

    @Test
    fun `Legg til fritekster til vedtakbegrunnelser`() {
        val fritekst1 = "fritekst1"
        val fritekst2 = "fritekst2"

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

        assertTrue(vedtak.vedtakBegrunnelser.any { it.brevBegrunnelse == fritekst1 })
        assertTrue(vedtak.vedtakBegrunnelser.any { it.brevBegrunnelse == fritekst2 })
    }

    @Test
    fun `Sjekk at gamle fritekster blir overskrevet når nye blir lagt til vedtakbegrunnelser`() {
        val fritekst1 = "fritekst1"
        val fritekst2 = "fritekst2"
        val fritekst3 = "fritekst3"
        val fritekst4 = "fritekst4"

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling)
        vedtakService.settFritekstbegrunnelserPåVedtaksperiodeOgType(
                fagsakId = fagsak.id,
                restPostFritekstVedtakBegrunnelser = RestPostFritekstVedtakBegrunnelser(fom = LocalDate.now().minusMonths(1),
                                                                                        tom = LocalDate.now(),
                                                                                        fritekster = listOf(fritekst1,
                                                                                                            fritekst2),
                                                                                        vedtaksperiodetype = Vedtaksperiodetype.OPPHØR),
                validerKombinasjoner = false)

        val vedtak = vedtakService.settFritekstbegrunnelserPåVedtaksperiodeOgType(
                fagsakId = fagsak.id,
                restPostFritekstVedtakBegrunnelser = RestPostFritekstVedtakBegrunnelser(fom = LocalDate.now().minusMonths(1),
                                                                                        tom = LocalDate.now(),
                                                                                        fritekster = listOf(fritekst3,
                                                                                                            fritekst4),
                                                                                        vedtaksperiodetype = Vedtaksperiodetype.OPPHØR),
                validerKombinasjoner = false)

        Assertions.assertFalse(vedtak.vedtakBegrunnelser.any { it.brevBegrunnelse == fritekst1 })
        Assertions.assertFalse(vedtak.vedtakBegrunnelser.any { it.brevBegrunnelse == fritekst2 })
        assertTrue(vedtak.vedtakBegrunnelser.any { it.brevBegrunnelse == fritekst3 })
        assertTrue(vedtak.vedtakBegrunnelser.any { it.brevBegrunnelse == fritekst4 })
    }

    @Test
    fun `valider opphør fritekst trenger begrunnelse av tilsvarende type og for samme periode`() {
        val fritekst1 = "fritekst1"
        val fritekst2 = "fritekst2"

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling)

        Assertions.assertThrows(FunksjonellFeil::class.java) {
            vedtakService.settFritekstbegrunnelserPåVedtaksperiodeOgType(
                    fagsakId = fagsak.id,
                    restPostFritekstVedtakBegrunnelser = RestPostFritekstVedtakBegrunnelser(fom = LocalDate.now()
                            .minusMonths(1),
                                                                                            tom = LocalDate.now(),
                                                                                            fritekster = listOf(fritekst1,
                                                                                                                fritekst2),
                                                                                            vedtaksperiodetype = Vedtaksperiodetype.OPPHØR))
        }
    }

    @Test
    fun `valider reduksjon fritekst trenger begrunnelse av tilsvarende type og for samme periode`() {
        val fritekst1 = "fritekst1"
        val fritekst2 = "fritekst2"

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling)

        Assertions.assertThrows(FunksjonellFeil::class.java) {
            vedtakService.settFritekstbegrunnelserPåVedtaksperiodeOgType(
                    fagsakId = fagsak.id,
                    restPostFritekstVedtakBegrunnelser = RestPostFritekstVedtakBegrunnelser(fom = LocalDate.now()
                            .minusMonths(1),
                                                                                            tom = LocalDate.now()
                                                                                                    .minusMonths(1),
                                                                                            fritekster = listOf(fritekst1,
                                                                                                                fritekst2),
                                                                                            vedtaksperiodetype = Vedtaksperiodetype.UTBETALING))
        }
    }

    @Test
    fun `valider at siste vanlige begrunnelse ikke kan slettes dersom det finnes fritekstbegrunnelse på periode og type`() {
        val fritekst1 = "fritekst1"
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
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(Vilkårsvurdering(behandling = behandling))
        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling)

        val begrunnelser =
                vedtakService.leggTilVedtakBegrunnelse(restPostVedtakBegrunnelse = RestPostVedtakBegrunnelse(
                        fom = LocalDate.now().minusMonths(1),
                        tom = LocalDate.now().minusMonths(1),
                        vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR
                ), fagsakId = fagsak.id)

        vedtakService.settFritekstbegrunnelserPåVedtaksperiodeOgType(
                fagsakId = fagsak.id,
                restPostFritekstVedtakBegrunnelser = RestPostFritekstVedtakBegrunnelser(fom = LocalDate.now().minusMonths(1),
                                                                                        tom = LocalDate.now().minusMonths(1),
                                                                                        fritekster = listOf(fritekst1),
                                                                                        vedtaksperiodetype = Vedtaksperiodetype.UTBETALING))

        Assertions.assertThrows(FunksjonellFeil::class.java) {
            val begrunnelseId = begrunnelser.find { !it.begrunnelse.erFritekstBegrunnelse() }!!.id
            vedtakService.slettBegrunnelse(fagsakId = fagsak.id, begrunnelseId = begrunnelseId)
        }
    }

    @Test
    fun `valider avslag fritekst trenger ikke begrunnelse av tilsvarende type`() {
        val fritekst1 = "fritekst1"
        val fritekst2 = "fritekst2"

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling)
        val vedtak = vedtakService.settFritekstbegrunnelserPåVedtaksperiodeOgType(
                fagsakId = fagsak.id,
                restPostFritekstVedtakBegrunnelser = RestPostFritekstVedtakBegrunnelser(fom = LocalDate.now().minusMonths(1),
                                                                                        tom = LocalDate.now(),
                                                                                        fritekster = listOf(fritekst1,
                                                                                                            fritekst2),
                                                                                        vedtaksperiodetype = Vedtaksperiodetype.AVSLAG))

        assertTrue(vedtak.validerVedtakBegrunnelserForFritekstOpphørOgReduksjon())
    }

    @Test
    fun `valider opphør fritekst med begrunnelse av tilsvarende type og for samme periode validerer`() {
        val fritekst1 = "fritekst1"
        val fritekst2 = "fritekst2"

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling)
        val vedtak = vedtakService.settFritekstbegrunnelserPåVedtaksperiodeOgType(
                fagsakId = fagsak.id,
                restPostFritekstVedtakBegrunnelser = RestPostFritekstVedtakBegrunnelser(fom = LocalDate.now().minusMonths(1),
                                                                                        tom = LocalDate.now(),
                                                                                        fritekster = listOf(fritekst1,
                                                                                                            fritekst2),
                                                                                        vedtaksperiodetype = Vedtaksperiodetype.OPPHØR),
                validerKombinasjoner = false)

        vedtak.leggTilBegrunnelse(lagVedtakBegrunnesle(fom = LocalDate.now().minusMonths(1),
                                                       tom = LocalDate.now(),
                                                       vedtak = vedtak,
                                                       vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.OPPHØR_BARN_FLYTTET_FRA_SØKER))

        assertTrue(vedtak.validerVedtakBegrunnelserForFritekstOpphørOgReduksjon())
    }
}