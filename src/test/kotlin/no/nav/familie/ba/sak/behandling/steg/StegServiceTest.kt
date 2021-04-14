package no.nav.familie.ba.sak.behandling.steg

import io.mockk.verify
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.RestPostVedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.behandling.vedtak.Beslutning
import no.nav.familie.ba.sak.behandling.vedtak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.Vilkårsvurdering
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.vurderVilkårsvurderingTilInnvilget
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.mockHentPersoninfoForMedIdenter
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedClient
import no.nav.familie.ba.sak.infotrygd.domene.InfotrygdVedtakFeedDto
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevDTO
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.StatusFraOppdragTask
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate


@SpringBootTest
@ActiveProfiles("dev",
                "mock-totrinnkontroll",
                "mock-brev-klient",
                "mock-økonomi",
                "mock-pdl",
                "mock-infotrygd-feed",
                "mock-simulering")
@TestInstance(Lifecycle.PER_CLASS)
class StegServiceTest(
        @Autowired
        private val stegService: StegService,

        @Autowired
        private val vedtakService: VedtakService,

        @Autowired
        private val behandlingService: BehandlingService,

        @Autowired
        private val persongrunnlagService: PersongrunnlagService,

        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val mockPersonopplysningerService: PersonopplysningerService,

        @Autowired
        private val vilkårsvurderingService: VilkårsvurderingService,

        @Autowired
        private val databaseCleanupService: DatabaseCleanupService,

        @Autowired
        private val totrinnskontrollService: TotrinnskontrollService,

        @Autowired
        private val infotrygdFeedClient: InfotrygdFeedClient
) {

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Skal sette default-verdier på gift-vilkår for barn`() {
        val søkerFnr = ClientMocks.søkerFnr[0]
        val barnFnr1 = ClientMocks.barnFnr[0]
        val barnFnr2 = ClientMocks.barnFnr[1]

        val behandling = kjørStegprosessForFGB(
                tilSteg = StegType.REGISTRERE_SØKNAD,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(barnFnr1, barnFnr2),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService
        )

        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)!!
        assertEquals(Resultat.OPPFYLT,
                     vilkårsvurdering.personResultater.first { it.personIdent == barnFnr1 }.vilkårResultater
                             .single { it.vilkårType == Vilkår.GIFT_PARTNERSKAP }.resultat)
        assertEquals(Resultat.IKKE_VURDERT,
                     vilkårsvurdering.personResultater.first { it.personIdent == barnFnr2 }.vilkårResultater
                             .single { it.vilkårType == Vilkår.GIFT_PARTNERSKAP }.resultat)
    }

    @Test
    fun `Skal kjøre gjennom alle steg med datageneratoren`() {
        kjørStegprosessForFGB(
                tilSteg = StegType.BEHANDLING_AVSLUTTET,
                søkerFnr = randomFnr(),
                barnasIdenter = listOf(ClientMocks.barnFnr[0]),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService
        )
    }

    @Test
    fun `Skal håndtere steg for frontend ordinær behandling`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()

        mockHentPersoninfoForMedIdenter(mockPersonopplysningerService, søkerFnr, barnFnr)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling)
        assertEquals(FØRSTE_STEG, behandling.steg)

        val behandlingEtterPersongrunnlagSteg = stegService.håndterSøknad(behandling = behandling,
                                                                          restRegistrerSøknad = RestRegistrerSøknad(
                                                                                  søknad = lagSøknadDTO(søkerIdent = søkerFnr,
                                                                                                        barnasIdenter = listOf(
                                                                                                                barnFnr)),
                                                                                  bekreftEndringerViaFrontend = true))

        assertEquals(StegType.VILKÅRSVURDERING, behandlingEtterPersongrunnlagSteg.steg)

        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)!!
        val barn: Person =
                persongrunnlagService.hentAktiv(behandlingId = behandling.id)!!.barna.find { it.personIdent.ident == barnFnr }!!
        vurderVilkårsvurderingTilInnvilget(vilkårsvurdering, barn)
        vilkårsvurderingService.oppdater(vilkårsvurdering)

        vedtakService.leggTilVedtakBegrunnelse(
                RestPostVedtakBegrunnelse(
                        fom = LocalDate.parse("2020-02-01"),
                        tom = LocalDate.parse("2025-02-01"),
                        vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR),
                fagsakId = fagsak.id)

        val behandlingEtterVilkårsvurderingSteg = stegService.håndterVilkårsvurdering(behandlingEtterPersongrunnlagSteg)
        assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterVilkårsvurderingSteg.steg)

        val behandlingEtterSendTilBeslutter = stegService.håndterSendTilBeslutter(behandlingEtterVilkårsvurderingSteg, "1234")
        assertEquals(StegType.BESLUTTE_VEDTAK, behandlingEtterSendTilBeslutter.steg)

        val behandlingEtterBeslutteVedtak = stegService.håndterBeslutningForVedtak(behandlingEtterSendTilBeslutter,
                                                                                   RestBeslutningPåVedtak(beslutning = Beslutning.GODKJENT))
        assertEquals(StegType.IVERKSETT_MOT_OPPDRAG, behandlingEtterBeslutteVedtak.steg)

        val vedtak = vedtakService.hentAktivForBehandling(behandlingEtterBeslutteVedtak.id)
        val behandlingEtterIverksetteVedtak =
                stegService.håndterIverksettMotØkonomi(behandlingEtterBeslutteVedtak, IverksettingTaskDTO(
                        behandlingsId = behandlingEtterBeslutteVedtak.id,
                        vedtaksId = vedtak!!.id,
                        saksbehandlerId = "System",
                        personIdent = søkerFnr
                ))
        assertEquals(StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI, behandlingEtterIverksetteVedtak.steg)

        verify(exactly = 1) {
            infotrygdFeedClient.sendVedtakFeedTilInfotrygd(InfotrygdVedtakFeedDto(søkerFnr, LocalDate.now()))
        }

        val behandlingEtterStatusFraOppdrag =
                stegService.håndterStatusFraØkonomi(behandlingEtterIverksetteVedtak, StatusFraOppdragMedTask(
                        statusFraOppdragDTO = StatusFraOppdragDTO(fagsystem = FAGSYSTEM,
                                                                  personIdent = søkerFnr,
                                                                  behandlingsId = behandlingEtterIverksetteVedtak.id,
                                                                  vedtaksId = vedtak.id),
                        task = Task.nyTask(type = StatusFraOppdragTask.TASK_STEP_TYPE, payload = "")
                ))
        assertEquals(StegType.JOURNALFØR_VEDTAKSBREV, behandlingEtterStatusFraOppdrag.steg)

        val behandlingEtterJournalførtVedtak =
                stegService.håndterJournalførVedtaksbrev(behandlingEtterStatusFraOppdrag, JournalførVedtaksbrevDTO(
                        vedtakId = vedtak.id,
                        task = Task.nyTask(type = JournalførVedtaksbrevTask.TASK_STEP_TYPE, payload = "")
                ))
        assertEquals(StegType.DISTRIBUER_VEDTAKSBREV, behandlingEtterJournalførtVedtak.steg)

        val behandlingEtterDistribuertVedtak = stegService.håndterDistribuerVedtaksbrev(behandlingEtterJournalførtVedtak,
                                                                                        DistribuerVedtaksbrevDTO(behandlingId = behandling.id,
                                                                                                                 journalpostId = "1234",
                                                                                                                 personIdent = søkerFnr))
        assertEquals(StegType.FERDIGSTILLE_BEHANDLING, behandlingEtterDistribuertVedtak.steg)

        val behandlingEtterFerdigstiltBehandling = stegService.håndterFerdigstillBehandling(behandlingEtterDistribuertVedtak)
        assertEquals(StegType.BEHANDLING_AVSLUTTET, behandlingEtterFerdigstiltBehandling.steg)
        assertEquals(BehandlingStatus.AVSLUTTET, behandlingEtterFerdigstiltBehandling.status)
        assertEquals(FagsakStatus.LØPENDE, behandlingEtterFerdigstiltBehandling.fagsak.status)
    }

    @Test
    fun `Skal feile når man prøver å håndtere feil steg`() {
        val søkerFnr = randomFnr()

        mockHentPersoninfoForMedIdenter(mockPersonopplysningerService, søkerFnr, "")

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        assertEquals(FØRSTE_STEG, behandling.steg)

        assertThrows<IllegalStateException> {
            stegService.håndterVilkårsvurdering(behandling)
        }
    }

    @Test
    fun `Skal feile når man prøver å endre en avsluttet behandling`() {
        val søkerFnr = randomFnr()

        mockHentPersoninfoForMedIdenter(mockPersonopplysningerService, søkerFnr, "")

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling, aktiv = true)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)

        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BEHANDLING_AVSLUTTET))
        behandling.status = BehandlingStatus.AVSLUTTET
        val feil = assertThrows<IllegalStateException> {
            stegService.håndterSendTilBeslutter(behandling, "1234")
        }
        assertEquals("Behandlingen er avsluttet og stegprosessen kan ikke gjenåpnes", feil.message)
    }

    @Test
    fun `Skal feile når man prøver å noe annet enn å beslutte behandling når den er på dette steget`() {
        val søkerFnr = randomFnr()

        mockHentPersoninfoForMedIdenter(mockPersonopplysningerService, søkerFnr, "")

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling, aktiv = true)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)

        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
        behandling.status = BehandlingStatus.FATTER_VEDTAK
        assertThrows<IllegalStateException> {
            stegService.håndterSendTilBeslutter(behandling, "1234")
        }
    }

    @Test
    fun `Skal feile når man prøver å kalle beslutning-steget med feil status på behandling`() {
        val søkerFnr = randomFnr()

        mockHentPersoninfoForMedIdenter(mockPersonopplysningerService, søkerFnr, "")

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
        behandling.status = BehandlingStatus.IVERKSETTER_VEDTAK
        assertThrows<IllegalStateException> {
            stegService.håndterBeslutningForVedtak(behandling,
                                                   RestBeslutningPåVedtak(beslutning = Beslutning.GODKJENT, begrunnelse = null))
        }
    }

    @Test
    fun `Underkjent beslutning setter steg tilbake til send til beslutter`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()

        mockHentPersoninfoForMedIdenter(mockPersonopplysningerService, søkerFnr, barnFnr)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(lagVilkårsvurdering(søkerFnr, behandling, Resultat.OPPFYLT))
        behandling.endretAv = "1234"
        assertEquals(FØRSTE_STEG, behandling.steg)

        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling = behandling)
        behandling.behandlingStegTilstand.forEach { it.behandlingStegStatus = BehandlingStegStatus.UTFØRT }
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
        behandling.status = BehandlingStatus.FATTER_VEDTAK
        stegService.håndterBeslutningForVedtak(behandling,
                                               RestBeslutningPåVedtak(beslutning = Beslutning.UNDERKJENT, begrunnelse = "Feil"))

        val behandlingEtterPersongrunnlagSteg = behandlingService.hent(behandlingId = behandling.id)
        assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterPersongrunnlagSteg.steg)
    }

    @Test
    fun `Henlegge før behandling er sendt til beslutter`() {
        val vilkårsvurdertBehandling = kjørGjennomStegInkludertSimulering()

        val henlagtBehandling = stegService.håndterHenleggBehandling(
                vilkårsvurdertBehandling, RestHenleggBehandlingInfo(årsak = HenleggÅrsak.FEILAKTIG_OPPRETTET,
                                                                    begrunnelse = ""))
        Assertions.assertTrue(henlagtBehandling.behandlingStegTilstand.filter {
            it.behandlingSteg == StegType.HENLEGG_SØKNAD && it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
        }
                                      .firstOrNull() != null)
        Assertions.assertTrue(henlagtBehandling.behandlingStegTilstand.filter {
            it.behandlingSteg == StegType.FERDIGSTILLE_BEHANDLING && it.behandlingStegStatus == BehandlingStegStatus.IKKE_UTFØRT
        }
                                      .firstOrNull() != null)

        stegService.håndterFerdigstillBehandling(henlagtBehandling)

        val behandlingEtterFerdigstiltBehandling = behandlingService.hent(behandlingId = henlagtBehandling.id)
        assertEquals(StegType.BEHANDLING_AVSLUTTET, behandlingEtterFerdigstiltBehandling.steg)
    }

    @Test
    fun `Henlegge etter behandling er sendt til beslutter`() {
        val vilkårsvurdertBehandling = kjørGjennomStegInkludertSimulering()
        stegService.håndterSendTilBeslutter(vilkårsvurdertBehandling, "1234")

        val behandlingEtterSendTilBeslutter = behandlingService.hent(behandlingId = vilkårsvurdertBehandling.id)

        assertThrows<IllegalStateException> {
            stegService.håndterHenleggBehandling(behandlingEtterSendTilBeslutter,
                                                 RestHenleggBehandlingInfo(årsak = HenleggÅrsak.FEILAKTIG_OPPRETTET,
                                                                           begrunnelse = ""))
        }
    }

    private fun kjørGjennomStegInkludertSimulering(): Behandling {
        val søkerFnr = randomFnr()
        val barnFnr = ClientMocks.barnFnr[0]

        return kjørStegprosessForFGB(
                tilSteg = StegType.SIMULERING,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(barnFnr),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService
        )
    }
}