package no.nav.familie.ba.sak.behandling.vedtak

import io.mockk.MockKAnnotations
import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.BehandlingMetrikker
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakPersonRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.RestPutUtbetalingBegrunnelse
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vilkår.*
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.ba.sak.saksstatistikk.SaksstatistikkEventPublisher
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate

@SpringBootTest
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("mock-pdl", "postgres", "mock-arbeidsfordeling")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VedtakBegrunnelseTest(
        @Autowired
        private val behandlingRepository: BehandlingRepository,

        @Autowired
        private val behandlingMetrikker: BehandlingMetrikker,

        @Autowired
        private val vilkårsvurderingService: VilkårsvurderingService,

        @Autowired
        private val vedtakService: VedtakService,

        @Autowired
        private val persongrunnlagService: PersongrunnlagService,

        @Autowired
        private val beregningService: BeregningService,

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
        private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository
) {

    lateinit var behandlingService: BehandlingService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        behandlingService = BehandlingService(
                behandlingRepository,
                behandlingMetrikker,
                fagsakPersonRepository,
                beregningService,
                loggService,
                arbeidsfordelingService,
                saksstatistikkEventPublisher,
                oppgaveService
        )
    }

    @Test
    fun `endring av begrunnelse skal koble seg til korrekt vilkår`() {
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
        søkerPersonResultat.setVilkårResultater(setOf(
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

        barn1PersonResultat.setVilkårResultater(setOf(
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

        barn2PersonResultat.setVilkårResultater(setOf(
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

        vedtakService.lagreOgDeaktiverGammel(lagVedtak(behandling))

        val initertRestUtbetalingBegrunnelseLovligOpphold =
                vedtakService.leggTilUtbetalingBegrunnelse(periode = Periode(fom = LocalDate.of(2010, 1, 1),
                                                                             tom = LocalDate.of(2010, 6, 1)),
                                                           fagsakId = fagsak.id)

        val begrunnelserLovligOpphold =
                vedtakService.endreUtbetalingBegrunnelse(
                        RestPutUtbetalingBegrunnelse(vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE,
                                                     vedtakBegrunnelse = VedtakBegrunnelse.INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE),
                        fagsakId = fagsak.id,
                        utbetalingBegrunnelseId = initertRestUtbetalingBegrunnelseLovligOpphold[0].id!!)

        assert(begrunnelserLovligOpphold.size == 1)
        Assertions.assertEquals(
                "Du får barnetrygd fordi du og barn født 01.01.19 har oppholdstillatelse fra desember 2009.",
                begrunnelserLovligOpphold.firstOrNull { it.vedtakBegrunnelse == VedtakBegrunnelse.INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE }!!.brevBegrunnelse)

        val initertRestUtbetalingBegrunnelseBosattIRiket =
                vedtakService.leggTilUtbetalingBegrunnelse(periode = Periode(fom = LocalDate.of(2010, 1, 1),
                                                                             tom = LocalDate.of(2010, 6, 1)),
                                                           fagsakId = fagsak.id)

        val begrunnelserLovligOppholdOgBosattIRiket =
                vedtakService.endreUtbetalingBegrunnelse(
                        RestPutUtbetalingBegrunnelse(vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE,
                                                     vedtakBegrunnelse = VedtakBegrunnelse.INNVILGET_BOSATT_I_RIKTET),
                        fagsakId = fagsak.id,
                        utbetalingBegrunnelseId = initertRestUtbetalingBegrunnelseBosattIRiket[1].id!!)

        assert(begrunnelserLovligOppholdOgBosattIRiket.size == 2)
        Assertions.assertEquals(
                "Du får barnetrygd fordi du er bosatt i Norge fra desember 2009.",
                begrunnelserLovligOppholdOgBosattIRiket.firstOrNull { it.vedtakBegrunnelse == VedtakBegrunnelse.INNVILGET_BOSATT_I_RIKTET }!!.brevBegrunnelse)
    }

    @Test
    fun `Skal sette begrunnelseType ved endring hvor kun type er satt`() {
        val behandlingEtterVilkårsvurdering = kjørStegprosessForFGB(
                tilSteg = StegType.VILKÅRSVURDERING,
                søkerFnr = ClientMocks.søkerFnr[0],
                barnasIdenter = listOf(ClientMocks.barnFnr[0]),
                fagsakService = fagsakService,
                behandlingService = behandlingService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService
        )

        val initertRestUtbetalingBegrunnelse =
                vedtakService.leggTilUtbetalingBegrunnelse(periode = Periode(fom = LocalDate.of(2010, 1, 1),
                                                                             tom = LocalDate.of(2010, 6, 1)),
                                                           fagsakId = behandlingEtterVilkårsvurdering.fagsak.id)

        val innvilgetBegrunnelse =
                vedtakService.endreUtbetalingBegrunnelse(
                        RestPutUtbetalingBegrunnelse(vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGELSE,
                                                     vedtakBegrunnelse = null),
                        fagsakId = behandlingEtterVilkårsvurdering.fagsak.id,
                        utbetalingBegrunnelseId = initertRestUtbetalingBegrunnelse.first().id!!)

        assert(innvilgetBegrunnelse.size == 1)
        Assertions.assertEquals(
                VedtakBegrunnelseType.INNVILGELSE,
                innvilgetBegrunnelse.first().begrunnelseType)
    }

    @Test
    fun `Endring av begrunnelse for redukasjon grunnet fylte 18 år skal koble seg til korrekt vilkår`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = Vilkårsvurdering(
                behandling = behandling
        )

        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barnFnr)

        barnPersonResultat.setVilkårResultater(setOf(
                VilkårResultat(personResultat = barnPersonResultat,
                               vilkårType = Vilkår.LOVLIG_OPPHOLD,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = LocalDate.of(2010, 12, 24),
                               periodeTom = LocalDate.of(2028, 12, 24),
                               begrunnelse = "",
                               behandlingId = vilkårsvurdering.behandling.id,
                               regelInput = null,
                               regelOutput = null)))


        vilkårsvurdering.personResultater = setOf(barnPersonResultat)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering)

        vedtakService.lagreOgDeaktiverGammel(lagVedtak(behandling))

        val utbetalingBegrunnelse18årMånedFør =
                vedtakService.leggTilUtbetalingBegrunnelse(periode = Periode(fom = LocalDate.of(2028, 11, 1),
                                                                             tom = LocalDate.of(2035, 6, 30)),
                                                           fagsakId = fagsak.id)

        val utbetalingBegrunnelse18årMånedEtter =
                vedtakService.leggTilUtbetalingBegrunnelse(periode = Periode(fom = LocalDate.of(2028, 12, 1),
                                                                             tom = LocalDate.of(2035, 6, 30)),
                                                           fagsakId = fagsak.id)

        val begrunnelserLovligOppholdMånedFør =
                vedtakService.endreUtbetalingBegrunnelse(
                        RestPutUtbetalingBegrunnelse(vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON,
                                                     vedtakBegrunnelse = VedtakBegrunnelse.REDUKSJON_UNDER_18_ÅR),
                        fagsakId = fagsak.id,
                        utbetalingBegrunnelseId = utbetalingBegrunnelse18årMånedFør[0].id!!)

        val begrunnelserLovligOppholdMånedEtter =
                vedtakService.endreUtbetalingBegrunnelse(
                        RestPutUtbetalingBegrunnelse(vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON,
                                                     vedtakBegrunnelse = VedtakBegrunnelse.REDUKSJON_UNDER_18_ÅR),
                        fagsakId = fagsak.id,
                        utbetalingBegrunnelseId = utbetalingBegrunnelse18årMånedEtter[0].id!!)

        assert(begrunnelserLovligOppholdMånedFør.size == 1)
        Assertions.assertEquals(
                "Barnetrygden reduseres fordi barn født  fylte 18 år.",
                begrunnelserLovligOppholdMånedFør.firstOrNull { it.vedtakBegrunnelse == VedtakBegrunnelse.REDUKSJON_UNDER_18_ÅR }!!.brevBegrunnelse)

        assert(begrunnelserLovligOppholdMånedEtter.size == 0)

    }
}