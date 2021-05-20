package no.nav.familie.ba.sak.behandling.vilkår

import io.mockk.MockKAnnotations
import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.BehandlingMetrikker
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakPersonRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.behandling.restDomene.RestPostFritekstVedtakBegrunnelser
import no.nav.familie.ba.sak.behandling.restDomene.RestPostVedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.behandling.vedtak.VedtakRepository
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.ba.sak.saksstatistikk.SaksstatistikkEventPublisher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
@ActiveProfiles("mock-pdl", "postgres", "mock-arbeidsfordeling", "mock-infotrygd-barnetrygd")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AvslagBegrunnelseOppdateringTest(
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
        private val vilkårService: VilkårService,

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
        private val infotrygdService: InfotrygdService,
) {

    lateinit var behandlingService: BehandlingService

    val barnFnr = randomFnr()
    lateinit var fagsak: Fagsak
    lateinit var behandling: Behandling
    lateinit var vilkårResultatInnvilget: VilkårResultat
    lateinit var vilkårResultatAvslag: VilkårResultat

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

        val søkerFnr = randomFnr()

        fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = Vilkårsvurdering(
                behandling = behandling
        )

        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barnFnr)

        vilkårResultatInnvilget = VilkårResultat(personResultat = barnPersonResultat,
                                                 vilkårType = Vilkår.BOSATT_I_RIKET,
                                                 resultat = Resultat.OPPFYLT,
                                                 periodeFom = LocalDate.of(2009, 12, 24),
                                                 periodeTom = LocalDate.of(2010, 1, 31),
                                                 begrunnelse = "",
                                                 behandlingId = vilkårsvurdering.behandling.id,
                                                 regelInput = null,
                                                 regelOutput = null)
        vilkårResultatAvslag = VilkårResultat(personResultat = barnPersonResultat,
                                              vilkårType = Vilkår.BOSATT_I_RIKET,
                                              resultat = Resultat.IKKE_OPPFYLT,
                                              periodeFom = LocalDate.of(2010, 1, 2),
                                              periodeTom = LocalDate.of(2010, 5, 15),
                                              begrunnelse = "",
                                              behandlingId = vilkårsvurdering.behandling.id,
                                              regelInput = null,
                                              regelOutput = null,
                                              erEksplisittAvslagPåSøknad = true)
        barnPersonResultat.setSortedVilkårResultater(setOf(
                vilkårResultatInnvilget, vilkårResultatAvslag))

        vilkårsvurdering.personResultater = setOf(barnPersonResultat)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering)

        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling)
    }

    @Test
    fun `Ny avslagbegrunnelser legges til ved oppdatering av av vilkår`() {
        vilkårService.endreVilkår(behandlingId = behandling.id,
                                  vilkårId = vilkårResultatAvslag.id,
                                  restPersonResultat = RestPersonResultat(personIdent = barnFnr,
                                                                          vilkårResultater = listOf(vilkårResultatAvslag.tilRestVilkårResultat(
                                                                                  avslagsbegrunnelser = listOf(
                                                                                          VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET)))))
        val fastsattBegrunnelse =
                vedtakService.hentAktivForBehandling(behandlingId = behandling.id)?.vedtakBegrunnelser?.singleOrNull()
        assertEquals(VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET,
                     fastsattBegrunnelse?.begrunnelse)
    }

    @Test
    fun `Eksisterende avslagbegrunnelse endres ved oppdatering av av vilkår`() {
        vilkårService.endreVilkår(behandlingId = behandling.id,
                                  vilkårId = vilkårResultatAvslag.id,
                                  restPersonResultat = RestPersonResultat(personIdent = barnFnr,
                                                                          vilkårResultater = listOf(vilkårResultatAvslag.tilRestVilkårResultat(
                                                                                  avslagsbegrunnelser = listOf(
                                                                                          VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET)))))
        assertEquals(vilkårResultatAvslag.vedtaksperiodeFom,
                     vedtakService.hentAktivForBehandling(behandlingId = behandling.id)?.vedtakBegrunnelser?.singleOrNull()?.fom)

        val oppdatertTomDato = LocalDate.now().plusMonths(1).sisteDagIMåned()
        vilkårService.endreVilkår(behandlingId = behandling.id,
                                  vilkårId = vilkårResultatAvslag.id,
                                  restPersonResultat = RestPersonResultat(personIdent = barnFnr,
                                                                          vilkårResultater = listOf(vilkårResultatAvslag.tilRestVilkårResultat(
                                                                                  periodeTom = oppdatertTomDato,
                                                                                  avslagsbegrunnelser = listOf(
                                                                                          VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET)))))
        val fastsattBegrunnelse =
                vedtakService.hentAktivForBehandling(behandlingId = behandling.id)?.vedtakBegrunnelser?.singleOrNull()
        assertEquals(VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET,
                     fastsattBegrunnelse?.begrunnelse)
        assertEquals(oppdatertTomDato,
                     fastsattBegrunnelse?.tom)
    }

    @Test
    fun `Eksisterende avslagbegrunnelse slettes ved oppdatering av av vilkår`() {
        vilkårService.endreVilkår(behandlingId = behandling.id,
                                  vilkårId = vilkårResultatAvslag.id,
                                  restPersonResultat = RestPersonResultat(personIdent = barnFnr,
                                                                          vilkårResultater = listOf(vilkårResultatAvslag.tilRestVilkårResultat(
                                                                                  avslagsbegrunnelser = listOf(
                                                                                          VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET)))))
        assertTrue(vedtakService.hentAktivForBehandling(behandlingId = behandling.id)!!.vedtakBegrunnelser.isNotEmpty())

        vilkårService.endreVilkår(behandlingId = behandling.id,
                                  vilkårId = vilkårResultatAvslag.id,
                                  restPersonResultat = RestPersonResultat(personIdent = barnFnr,
                                                                          vilkårResultater = listOf(vilkårResultatAvslag.tilRestVilkårResultat(
                                                                                  avslagsbegrunnelser = emptyList()))))
        assertTrue(vedtakService.hentAktivForBehandling(behandlingId = behandling.id)!!.vedtakBegrunnelser.isEmpty())
    }

    @Test
    fun `Eksisterende avslagbegrunnelse slettes ved sletting av av vilkår`() {
        vilkårService.endreVilkår(behandlingId = behandling.id,
                                  vilkårId = vilkårResultatAvslag.id,
                                  restPersonResultat = RestPersonResultat(personIdent = barnFnr,
                                                                          vilkårResultater = listOf(vilkårResultatAvslag.tilRestVilkårResultat(
                                                                                  avslagsbegrunnelser = listOf(
                                                                                          VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET)))))
        assertTrue(vedtakService.hentAktivForBehandling(behandlingId = behandling.id)!!.vedtakBegrunnelser.isNotEmpty())
        vilkårService.deleteVilkår(behandlingId = behandling.id,
                                   vilkårId = vilkårResultatAvslag.id,
                                   personIdent = barnFnr)
        assertTrue(vedtakService.hentAktivForBehandling(behandlingId = behandling.id)!!.vedtakBegrunnelser.isEmpty())
    }

    @Test
    fun `Avslagbegrunnelser slettes ikke ved resetting av begrunnelser for utbetalingsperioder og opphørsperioder`() {
        vedtakService.leggTilVedtakBegrunnelse(restPostVedtakBegrunnelse = RestPostVedtakBegrunnelse(
                fom = LocalDate.of(2010, 1, 1),
                tom = LocalDate.of(2010, 2, 28),
                vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET
        ), fagsakId = fagsak.id)
        vedtakService.oppdaterAvslagBegrunnelserForVilkår(behandlingId = behandling.id,
                                                          vilkårResultat = vilkårResultatAvslag,
                                                          begrunnelser = listOf(VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET))
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)

        assertEquals(2, vedtak?.vedtakBegrunnelser?.size)
        assertEquals(setOf(VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET,
                           VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET),
                     vedtak?.vedtakBegrunnelser?.map { it.begrunnelse }?.toSet())

        vedtakService.settStegSlettVedtakBegrunnelserOgTilbakekreving(behandlingId = behandling.id)

        assertEquals(VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET,
                     vedtakService.hentAktivForBehandling(behandlingId = behandling.id)?.vedtakBegrunnelser?.singleOrNull()?.begrunnelse)
    }

    @Test
    fun `Avslagbegrunnelser av typen fritekst skall slettes ved resetting av begrunnelser for utbetalingsperioder og opphørsperioder`() {

        val restPostFritekstVedtakBegrunnelser =
                RestPostFritekstVedtakBegrunnelser(fom = null, tom = null, fritekster = listOf("teks1"),
                                                   vedtaksperiodetype = Vedtaksperiodetype.AVSLAG)

        vedtakService.settFritekstbegrunnelserPåVedtaksperiodeOgType(fagsakId = fagsak.id,
                                                                     restPostFritekstVedtakBegrunnelser = restPostFritekstVedtakBegrunnelser)

        assertTrue(vedtakService.hentAktivForBehandling(behandlingId = behandling.id)?.vedtakBegrunnelser?.size == 1)

        vedtakService.settStegSlettVedtakBegrunnelserOgTilbakekreving(behandlingId = behandling.id)

        assertTrue(vedtakService.hentAktivForBehandling(behandlingId = behandling.id)?.vedtakBegrunnelser?.size == 0)
    }

    @Test
    fun `Avslagbegrunnelser slettes når tilhørende periode forsvinner som følge av overlappende innvilget periode`() {
        vilkårService.endreVilkår(behandlingId = behandling.id,
                                  vilkårId = vilkårResultatAvslag.id,
                                  restPersonResultat = RestPersonResultat(personIdent = barnFnr,
                                                                          vilkårResultater = listOf(vilkårResultatAvslag.tilRestVilkårResultat(
                                                                                  avslagsbegrunnelser = listOf(
                                                                                          VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET)))))
        assertTrue(vedtakService.hentAktivForBehandling(behandlingId = behandling.id)!!.vedtakBegrunnelser.isNotEmpty())

        vilkårService.endreVilkår(behandlingId = behandling.id,
                                  vilkårId = vilkårResultatInnvilget.id,
                                  restPersonResultat = RestPersonResultat(personIdent = barnFnr,
                                                                          vilkårResultater = listOf(vilkårResultatInnvilget.tilRestVilkårResultat(
                                                                                  periodeTom = LocalDate.of(2010, 7, 15),
                                                                                  avslagsbegrunnelser = emptyList()))))
        assertTrue(vedtakService.hentAktivForBehandling(behandlingId = behandling.id)!!.vedtakBegrunnelser.isEmpty())
    }

    @Test
    fun `Oppdatering av avslagbegrunnelse som ikke samsvarer med vilkår kaster feil`() {
        assertThrows<IllegalStateException> {
            vedtakService.oppdaterAvslagBegrunnelserForVilkår(behandlingId = behandling.id,
                                                              vilkårResultat = vilkårResultatAvslag,
                                                              begrunnelser = listOf(VedtakBegrunnelseSpesifikasjon.AVSLAG_LOVLIG_OPPHOLD_EØS_BORGER))
        }
    }

    @Test
    fun `Avslagbegrunnelser mappes til korrekt RestVilkårResultat`() {
        vedtakService.oppdaterAvslagBegrunnelserForVilkår(behandlingId = behandling.id,
                                                          vilkårResultat = vilkårResultatAvslag,
                                                          begrunnelser = listOf(VedtakBegrunnelseSpesifikasjon.AVSLAG_MEDLEM_I_FOLKETRYGDEN))

        val restBehandling = fagsakService.hentRestFagsak(fagsakId = fagsak.id).data!!.behandlinger.first()

        val restVilkårResultater = restBehandling.personResultater.find { it.personIdent == barnFnr }?.vilkårResultater
        assertEquals(2, restVilkårResultater?.size)
        assertEquals(emptyList<VedtakBegrunnelseSpesifikasjon>(),
                     restVilkårResultater?.find { it.resultat == Resultat.OPPFYLT }?.avslagBegrunnelser)
        assertEquals(listOf(VedtakBegrunnelseSpesifikasjon.AVSLAG_MEDLEM_I_FOLKETRYGDEN),
                     restVilkårResultater?.find { it.resultat == Resultat.IKKE_OPPFYLT }?.avslagBegrunnelser)
    }

    private fun VilkårResultat.tilRestVilkårResultat(periodeFom: LocalDate? = this.periodeFom,
                                                     periodeTom: LocalDate? = this.periodeTom,
                                                     avslagsbegrunnelser: List<VedtakBegrunnelseSpesifikasjon>?) =
            RestVilkårResultat(
                    id = this.id,
                    vilkårType = this.vilkårType,
                    resultat = this.resultat,
                    periodeFom = periodeFom,
                    periodeTom = periodeTom,
                    begrunnelse = this.begrunnelse,
                    behandlingId = this.behandlingId,
                    erEksplisittAvslagPåSøknad = this.erEksplisittAvslagPåSøknad,
                    endretAv = this.endretAv,
                    endretTidspunkt = this.endretTidspunkt,
                    avslagBegrunnelser = avslagsbegrunnelser)
}