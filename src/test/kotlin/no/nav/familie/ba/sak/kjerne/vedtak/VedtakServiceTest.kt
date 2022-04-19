package no.nav.familie.ba.sak.kjerne.vedtak

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingMetrikker
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingMigreringsinfoRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.TidslinjeService
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat.Companion.VilkårResultatComparator
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
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
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,

    @Autowired
    private val behandlingstemaService: BehandlingstemaService,

    @Autowired
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService,

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
    private val personidentService: PersonidentService,

    @Autowired
    private val persongrunnlagService: PersongrunnlagService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val fagsakRepository: FagsakRepository,

    @Autowired
    private val totrinnskontrollService: TotrinnskontrollService,

    @Autowired
    private val loggService: LoggService,

    @Autowired
    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,

    @Autowired
    private val infotrygdService: InfotrygdService,

    @Autowired
    private val featureToggleService: FeatureToggleService,

    @Autowired
    private val beregningService: BeregningService,

    @Autowired
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,

    @Autowired
    private val taskRepository: TaskRepositoryWrapper,

    @Autowired
    private val behandlingMigreringsinfoRepository: BehandlingMigreringsinfoRepository,

    @Autowired
    private val behandlingSøknadsinfoRepository: BehandlingSøknadsinfoRepository,

    @Autowired
    private val tidslinjeService: TidslinjeService,

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
        behandlingService = BehandlingService(
            behandlingRepository,
            behandlingHentOgPersisterService,
            behandlingstemaService,
            behandlingSøknadsinfoService,
            behandlingMigreringsinfoRepository,
            behandlingMetrikker,
            saksstatistikkEventPublisher,
            fagsakRepository,
            vedtakRepository,
            loggService,
            arbeidsfordelingService,
            infotrygdService,
            vedtaksperiodeService,
            personidentService,
            featureToggleService,
            taskRepository,
            vilkårsvurderingService,
        )

        val personAktørId = randomAktørId()

        behandling = lagBehandling()

        vilkår = Vilkår.LOVLIG_OPPHOLD
        resultat = Resultat.OPPFYLT

        vilkårsvurdering = lagVilkårsvurdering(personAktørId, behandling, resultat)

        personResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = personAktørId
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

        val fnrAktørNr = personidentService.hentAktør(fnr)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)

        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val vilkårsvurdering = lagVilkårsvurdering(fnrAktørNr, behandling, Resultat.OPPFYLT)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)

        val barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true)
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                fnr,
                listOf(barnFnr),
                søkerAktør = fagsak.aktør,
                barnAktør = barnAktør
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling = behandling)

        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(
            behandling,
            "ansvarligSaksbehandler",
            "saksbehandlerId"
        )
        totrinnskontrollService.besluttTotrinnskontroll(
            behandling,
            "ansvarligBeslutter",
            "beslutterId",
            Beslutning.GODKJENT
        )

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
