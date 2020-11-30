package no.nav.familie.ba.sak.behandling.steg

import io.mockk.verify
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.behandling.vedtak.Beslutning
import no.nav.familie.ba.sak.behandling.vedtak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultat
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.vurderBehandlingResultatTilInnvilget
import no.nav.familie.ba.sak.config.mockHentPersoninfoForMedIdenter
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedClient
import no.nav.familie.ba.sak.infotrygd.domene.InfotrygdVedtakFeedDto
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevDTO
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.StatusFraOppdragTask
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate


@SpringBootTest
@ActiveProfiles("dev", "mock-totrinnkontroll", "mock-dokgen", "mock-økonomi", "mock-pdl", "mock-infotrygd-feed")
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
        private val behandlingResultatService: BehandlingResultatService,

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
    fun `Skal håndtere steg for frontend ordinær behandling`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()

        mockHentPersoninfoForMedIdenter(mockPersonopplysningerService, søkerFnr, barnFnr)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        Assertions.assertEquals(FØRSTE_STEG, behandling.steg)

        val behandlingEtterPersongrunnlagSteg = stegService.håndterSøknad(behandling = behandling,
                                  restRegistrerSøknad = RestRegistrerSøknad(
                                          søknad = lagSøknadDTO(søkerIdent = søkerFnr,
                                                                barnasIdenter = listOf(barnFnr)),
                                          bekreftEndringerViaFrontend = true))

        Assertions.assertEquals(StegType.VILKÅRSVURDERING, behandlingEtterPersongrunnlagSteg.steg)

        val behandlingResultat = behandlingResultatService.hentAktivForBehandling(behandlingId = behandling.id)!!
        val barn: Person =
                persongrunnlagService.hentAktiv(behandlingId = behandling.id)!!.barna.find { it.personIdent.ident == barnFnr }!!
        vurderBehandlingResultatTilInnvilget(behandlingResultat, barn)
        behandlingResultatService.oppdater(behandlingResultat)

        val behandlingEtterVilkårsvurderingSteg = stegService.håndterVilkårsvurdering(behandlingEtterPersongrunnlagSteg)
        Assertions.assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterVilkårsvurderingSteg.steg)

        val behandlingEtterSendTilBeslutter =  stegService.håndterSendTilBeslutter(behandlingEtterVilkårsvurderingSteg, "1234")
        Assertions.assertEquals(StegType.BESLUTTE_VEDTAK, behandlingEtterSendTilBeslutter.steg)

        val behandlingEtterBeslutteVedtak = stegService.håndterBeslutningForVedtak(behandlingEtterSendTilBeslutter,
                                               RestBeslutningPåVedtak(beslutning = Beslutning.GODKJENT))
        Assertions.assertEquals(StegType.IVERKSETT_MOT_OPPDRAG, behandlingEtterBeslutteVedtak.steg)

        val vedtak = vedtakService.hentAktivForBehandling(behandlingEtterBeslutteVedtak.id)
        val behandlingEtterIverksetteVedtak = stegService.håndterIverksettMotØkonomi(behandlingEtterBeslutteVedtak, IverksettingTaskDTO(
                behandlingsId = behandlingEtterBeslutteVedtak.id,
                vedtaksId = vedtak!!.id,
                saksbehandlerId = "System",
                personIdent = søkerFnr
        ))
        Assertions.assertEquals(StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI, behandlingEtterIverksetteVedtak.steg)

        verify(exactly = 1) {
            infotrygdFeedClient.sendVedtakFeedTilInfotrygd(InfotrygdVedtakFeedDto(søkerFnr, LocalDate.now()))
        }

        val behandlingEtterStatusFraOppdrag = stegService.håndterStatusFraØkonomi(behandlingEtterIverksetteVedtak, StatusFraOppdragMedTask(
                statusFraOppdragDTO = StatusFraOppdragDTO(fagsystem = FAGSYSTEM,
                                                          personIdent = søkerFnr,
                                                          behandlingsId = behandlingEtterIverksetteVedtak.id,
                                                          vedtaksId = vedtak.id),
                task = Task.nyTask(type = StatusFraOppdragTask.TASK_STEP_TYPE, payload = "")
        ))
        Assertions.assertEquals(StegType.JOURNALFØR_VEDTAKSBREV, behandlingEtterStatusFraOppdrag.steg)

        val behandlingEtterJournalførtVedtak = stegService.håndterJournalførVedtaksbrev(behandlingEtterStatusFraOppdrag, JournalførVedtaksbrevDTO(
                vedtakId = vedtak.id,
                task = Task.nyTask(type = JournalførVedtaksbrevTask.TASK_STEP_TYPE, payload = "")
        ))
        Assertions.assertEquals(StegType.DISTRIBUER_VEDTAKSBREV, behandlingEtterJournalførtVedtak.steg)

        val behandlingEtterDistribuertVedtak = stegService.håndterDistribuerVedtaksbrev(behandlingEtterJournalførtVedtak,
                                                 DistribuerVedtaksbrevDTO(behandlingId = behandling.id,
                                                                          journalpostId = "1234",
                                                                          personIdent = søkerFnr))
        Assertions.assertEquals(StegType.FERDIGSTILLE_BEHANDLING, behandlingEtterDistribuertVedtak.steg)

        val behandlingEtterFerdigstiltBehandling = stegService.håndterFerdigstillBehandling(behandlingEtterDistribuertVedtak)
        Assertions.assertEquals(StegType.BEHANDLING_AVSLUTTET, behandlingEtterFerdigstiltBehandling.steg)
        Assertions.assertEquals(BehandlingStatus.AVSLUTTET, behandlingEtterFerdigstiltBehandling.status)
        Assertions.assertEquals(FagsakStatus.LØPENDE, behandlingEtterFerdigstiltBehandling.fagsak.status)
    }

    @Test
    fun `Skal feile når man prøver å håndtere feil steg`() {
        val søkerFnr = randomFnr()

        mockHentPersoninfoForMedIdenter(mockPersonopplysningerService, søkerFnr, "")

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        Assertions.assertEquals(FØRSTE_STEG,
                                behandling.steg)

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
        val behandlingResultat = BehandlingResultat(behandling = behandling, aktiv = true)

        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat = behandlingResultat)

        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BEHANDLING_AVSLUTTET))
        behandling.status = BehandlingStatus.AVSLUTTET
        assertThrows<IllegalStateException> {
            stegService.håndterSendTilBeslutter(behandling, "1234")
        }
    }

    @Test
    fun `Skal feile når man prøver å noe annet enn å beslutte behandling når den er på dette steget`() {
        val søkerFnr = randomFnr()

        mockHentPersoninfoForMedIdenter(mockPersonopplysningerService, søkerFnr, "")

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val behandlingResultat = BehandlingResultat(behandling = behandling, aktiv = true)

        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat = behandlingResultat)

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
        behandling.endretAv = "1234"
        Assertions.assertEquals(FØRSTE_STEG, behandling.steg)

        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling = behandling)
        behandling.behandlingStegTilstand.forEach{ it.behandlingStegStatus = BehandlingStegStatus.UTFØRT}
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
        behandling.status = BehandlingStatus.FATTER_VEDTAK
        stegService.håndterBeslutningForVedtak(behandling,
                                               RestBeslutningPåVedtak(beslutning = Beslutning.UNDERKJENT, begrunnelse = "Feil"))

        val behandlingEtterPersongrunnlagSteg = behandlingService.hent(behandlingId = behandling.id)
        Assertions.assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterPersongrunnlagSteg.steg)
    }

    @Test
    fun `Henlegge før behandling er sendt til beslutter`() {
        val vilkårsvurdertBehandling = kjørGjennomStegInkludertVilkårsvurdering()

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
        Assertions.assertEquals(StegType.BEHANDLING_AVSLUTTET, behandlingEtterFerdigstiltBehandling.steg)
    }

    @Test
    fun `Henlegge etter behandling er sendt til beslutter`() {
        val vilkårsvurdertBehandling = kjørGjennomStegInkludertVilkårsvurdering()
        stegService.håndterSendTilBeslutter(vilkårsvurdertBehandling, "1234")

        val behandlingEtterSendTilBeslutter = behandlingService.hent(behandlingId = vilkårsvurdertBehandling.id)

        assertThrows<IllegalStateException> {
            stegService.håndterHenleggBehandling(behandlingEtterSendTilBeslutter,
                                                 RestHenleggBehandlingInfo(årsak = HenleggÅrsak.FEILAKTIG_OPPRETTET,
                                                                           begrunnelse = ""))
        }
    }

    private fun kjørGjennomStegInkludertVilkårsvurdering(): Behandling {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()

        mockHentPersoninfoForMedIdenter(mockPersonopplysningerService, søkerFnr, barnFnr)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        stegService.håndterSøknad(behandling = behandling,
                                  restRegistrerSøknad = RestRegistrerSøknad(
                                          søknad = lagSøknadDTO(søkerIdent = søkerFnr,
                                                                barnasIdenter = listOf(barnFnr)),
                                          bekreftEndringerViaFrontend = true))

        val behandlingEtterPersongrunnlagSteg = behandlingService.hent(behandlingId = behandling.id)
        val behandlingResultat = behandlingResultatService.hentAktivForBehandling(behandlingId = behandling.id)!!
        val barn: Person =
                persongrunnlagService.hentAktiv(behandlingId = behandling.id)!!.barna.find { it.personIdent.ident == barnFnr }!!
        vurderBehandlingResultatTilInnvilget(behandlingResultat, barn)
        behandlingResultatService.oppdater(behandlingResultat)
        stegService.håndterVilkårsvurdering(behandlingEtterPersongrunnlagSteg)
        return behandlingService.hent(behandlingId = behandling.id)
    }
}