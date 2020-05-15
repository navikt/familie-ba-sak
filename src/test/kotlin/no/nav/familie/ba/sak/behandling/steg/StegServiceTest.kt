package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.TypeSøker
import no.nav.familie.ba.sak.behandling.vedtak.Beslutning
import no.nav.familie.ba.sak.behandling.vedtak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.behandling.vedtak.RestVilkårsvurdering
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.vilkårsvurderingInnvilget
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.mockHentPersoninfoForMedIdenter
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevDTO
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.StatusFraOppdragTask
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate


@SpringBootTest
@ActiveProfiles("dev", "mock-totrinnkontroll", "mock-dokgen", "mock-iverksett")
@TestInstance(Lifecycle.PER_CLASS)
class StegServiceTest(
        @Autowired
        private val stegService: StegService,

        @Autowired
        private val vedtakService: VedtakService,

        @Autowired
        private val behandlingService: BehandlingService,

        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val mockIntegrasjonClient: IntegrasjonClient,

        @Autowired
        private val behandlingResultatService: BehandlingResultatService,

        @Autowired
        private val databaseCleanupService: DatabaseCleanupService
) {

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Skal håndtere steg for ordinær behandling`() {
        val søkerFnr = randomFnr()
        val annenPartIdent = randomFnr()
        val barnFnr = randomFnr()

        mockHentPersoninfoForMedIdenter(mockIntegrasjonClient, søkerFnr, barnFnr)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        Assertions.assertEquals(initSteg(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING), behandling.steg)

        stegService.håndterSøknad(behandling,
                                  lagSøknadDTO(annenPartIdent = annenPartIdent,
                                               søkerIdent = søkerFnr,
                                               barnasIdenter = listOf(barnFnr)))

        val behandlingEtterPersongrunnlagSteg = behandlingService.hent(behandlingId = behandling.id)
        Assertions.assertEquals(StegType.VILKÅRSVURDERING, behandlingEtterPersongrunnlagSteg.steg)

        stegService.håndterVilkårsvurdering(behandlingEtterPersongrunnlagSteg, RestVilkårsvurdering(
                personResultater = vilkårsvurderingInnvilget(søkerFnr, barnFnr, LocalDate.of(2018, 5, 1)))
        )

        val behandlingEtterVilkårsvurderingSteg = behandlingService.hent(behandlingId = behandling.id)
        Assertions.assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterVilkårsvurderingSteg.steg)

        stegService.håndterSendTilBeslutter(behandlingEtterVilkårsvurderingSteg)

        val behandlingEtterSendTilBeslutter = behandlingService.hent(behandlingId = behandling.id)
        Assertions.assertEquals(StegType.BESLUTTE_VEDTAK, behandlingEtterSendTilBeslutter.steg)

        stegService.håndterBeslutningForVedtak(behandlingEtterSendTilBeslutter,
                                               RestBeslutningPåVedtak(beslutning = Beslutning.GODKJENT))

        val behandlingEtterBeslutteVedtak = behandlingService.hent(behandlingId = behandling.id)
        Assertions.assertEquals(StegType.IVERKSETT_MOT_OPPDRAG, behandlingEtterBeslutteVedtak.steg)

        val vedtak = vedtakService.hentAktivForBehandling(behandlingEtterBeslutteVedtak.id)
        stegService.håndterIverksettMotØkonomi(behandlingEtterBeslutteVedtak, IverksettingTaskDTO(
                behandlingsId = behandlingEtterBeslutteVedtak.id,
                vedtaksId = vedtak!!.id,
                saksbehandlerId = "System",
                personIdent = søkerFnr
        ))

        val behandlingEtterIverksetteVedtak = behandlingService.hent(behandlingId = behandling.id)
        Assertions.assertEquals(StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI, behandlingEtterIverksetteVedtak.steg)

        stegService.håndterStatusFraØkonomi(behandlingEtterIverksetteVedtak, StatusFraOppdragMedTask(
                statusFraOppdragDTO = StatusFraOppdragDTO(fagsystem = FAGSYSTEM,
                                                          personIdent = søkerFnr,
                                                          behandlingsId = behandlingEtterIverksetteVedtak.id,
                                                          vedtaksId = vedtak.id),
                task = Task.nyTask(type = StatusFraOppdragTask.TASK_STEP_TYPE, payload = "")
        ))

        val behandlingEtterStatusFraOppdrag = behandlingService.hent(behandlingId = behandling.id)
        Assertions.assertEquals(StegType.JOURNALFØR_VEDTAKSBREV, behandlingEtterStatusFraOppdrag.steg)

        stegService.håndterJournalførVedtaksbrev(behandlingEtterStatusFraOppdrag, JournalførVedtaksbrevDTO(
                vedtakId = vedtak.id,
                task = Task.nyTask(type = JournalførVedtaksbrevTask.TASK_STEP_TYPE, payload = "")
        ))

        val behandlingEtterJournalførtVedtak = behandlingService.hent(behandlingId = behandling.id)
        Assertions.assertEquals(StegType.DISTRIBUER_VEDTAKSBREV, behandlingEtterJournalførtVedtak.steg)

        stegService.håndterDistribuerVedtaksbrev(behandlingEtterJournalførtVedtak,
                                                 DistribuerVedtaksbrevDTO(behandlingId = behandling.id,
                                                                          journalpostId = "1234",
                                                                          personIdent = søkerFnr))

        val behandlingEtterDistribuertVedtak = behandlingService.hent(behandlingId = behandling.id)
        Assertions.assertEquals(StegType.FERDIGSTILLE_BEHANDLING, behandlingEtterDistribuertVedtak.steg)

        stegService.håndterFerdigstillBehandling(behandlingEtterDistribuertVedtak)

        val behandlingEtterFerdigstiltBehandling = behandlingService.hent(behandlingId = behandling.id)
        Assertions.assertEquals(StegType.BEHANDLING_AVSLUTTET, behandlingEtterFerdigstiltBehandling.steg)
        Assertions.assertEquals(BehandlingStatus.FERDIGSTILT, behandlingEtterFerdigstiltBehandling.status)
        Assertions.assertEquals(FagsakStatus.LØPENDE, behandlingEtterFerdigstiltBehandling.fagsak.status)
    }

    @Test
    fun `Skal initiere vilkår for lovlig opphold basert på søkertype`() {
        assertLovligOppholdForTypeSøker(TypeSøker.TREDJELANDSBORGER, true)
        assertLovligOppholdForTypeSøker(TypeSøker.EØS_BORGER, true)
        assertLovligOppholdForTypeSøker(TypeSøker.ORDINÆR, false)
    }

    @Test
    fun `Skal feile når man prøver å håndtere feil steg`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        Assertions.assertEquals(initSteg(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING), behandling.steg)

        assertThrows<IllegalStateException> {
            stegService.håndterVilkårsvurdering(behandling, RestVilkårsvurdering(
                    personResultater = vilkårsvurderingInnvilget(søkerFnr, barnFnr, LocalDate.of(2019, 1, 1)))
            )
        }
    }

    @Test
    fun `Skal feile når man prøver å endre en avsluttet behandling`() {
        val søkerFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        behandling.steg = StegType.BEHANDLING_AVSLUTTET
        behandling.status = BehandlingStatus.FERDIGSTILT
        assertThrows<IllegalStateException> {
            stegService.håndterSendTilBeslutter(behandling)
        }
    }

    @Test
    fun `Skal feile når man prøver å noe annet enn å beslutte behandling når den er på dette steget`() {
        val søkerFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        behandling.steg = StegType.BESLUTTE_VEDTAK
        behandling.status = BehandlingStatus.SENDT_TIL_BESLUTTER
        assertThrows<IllegalStateException> {
            stegService.håndterSendTilBeslutter(behandling)
        }
    }

    @Test
    fun `Skal feile når man prøver å kalle beslutnin-steget med feil status på behandling`() {
        val søkerFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        behandling.steg = StegType.BESLUTTE_VEDTAK
        behandling.status = BehandlingStatus.SENDT_TIL_IVERKSETTING
        assertThrows<IllegalStateException> {
            stegService.håndterBeslutningForVedtak(behandling,
                                                   RestBeslutningPåVedtak(beslutning = Beslutning.GODKJENT, begrunnelse = null))
        }
    }

    @Test
    fun `Håndter underkjent beslutning`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()

        mockHentPersoninfoForMedIdenter(mockIntegrasjonClient, søkerFnr, barnFnr)


        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        behandling.endretAv = "1234"
        Assertions.assertEquals(initSteg(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING), behandling.steg)

        behandling.steg = StegType.BESLUTTE_VEDTAK
        behandling.status = BehandlingStatus.SENDT_TIL_BESLUTTER
        stegService.håndterBeslutningForVedtak(behandling,
                                               RestBeslutningPåVedtak(beslutning = Beslutning.UNDERKJENT, begrunnelse = "Feil"))

        val behandlingEtterPersongrunnlagSteg = behandlingService.hent(behandlingId = behandling.id)
        Assertions.assertEquals(StegType.REGISTRERE_SØKNAD, behandlingEtterPersongrunnlagSteg.steg)
    }

    private fun assertLovligOppholdForTypeSøker(typeSøker: TypeSøker, skalInkludereLovligOpphold: Boolean) {
        val søkerFnr = randomFnr()
        val annenPartIdent = randomFnr()
        val barnFnr = randomFnr()

        mockHentPersoninfoForMedIdenter(mockIntegrasjonClient, søkerFnr, barnFnr)


        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        Assertions.assertEquals(initSteg(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING), behandling.steg)

        stegService.håndterSøknad(behandling,
                                  lagSøknadDTO(annenPartIdent = annenPartIdent,
                                               søkerIdent = søkerFnr,
                                               barnasIdenter = listOf(barnFnr)).copy(typeSøker = typeSøker))
        val behandlingResultat = behandlingResultatService.hentAktivForBehandling(behandling.id)!!
        behandlingResultat.personResultater.forEach { personresultat ->
            Assertions.assertEquals(skalInkludereLovligOpphold, personresultat.vilkårResultater.any { vilkårResultat ->
                vilkårResultat.vilkårType == Vilkår.LOVLIG_OPPHOLD
            })
        }
    }
}