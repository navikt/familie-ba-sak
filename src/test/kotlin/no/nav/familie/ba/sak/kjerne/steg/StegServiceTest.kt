package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.verify
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.kjørStegprosessForRevurderingÅrligKontroll
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.leggTilBegrunnelsePåVedtaksperiodeIBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.vurderVilkårsvurderingTilInnvilget
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.config.mockHentPersoninfoForMedIdenter
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdFeedClient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.InfotrygdVedtakFeedDto
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.dokument.hentBrevtype
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.StatusFraOppdragTask
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.*


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
        private val infotrygdFeedClient: InfotrygdFeedClient,

        @Autowired
        private val tilbakekrevingService: TilbakekrevingService,

        @Autowired
        private val vedtaksperiodeService: VedtaksperiodeService,
) : AbstractSpringIntegrationTest() {

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
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
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
        val søkerFnr = randomFnr()
        kjørStegprosessForFGB(
                tilSteg = StegType.BEHANDLING_AVSLUTTET,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(ClientMocks.barnFnr[0]),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
        )

        // Venter med å kjøre gjennom til avsluttet til brev er støttet for fortsatt innvilget.
        kjørStegprosessForRevurderingÅrligKontroll(
                tilSteg = StegType.SEND_TIL_BESLUTTER,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(ClientMocks.barnFnr[0]),
                vedtakService = vedtakService,
                stegService = stegService,
                tilbakekrevingService = tilbakekrevingService
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

        val behandlingEtterVilkårsvurderingSteg = stegService.håndterVilkårsvurdering(behandlingEtterPersongrunnlagSteg)
        assertEquals(StegType.VURDER_TILBAKEKREVING, behandlingEtterVilkårsvurderingSteg.steg)

        val behandlingEtterVurderTilbakekrevingSteg = stegService.håndterVurderTilbakekreving(
                behandlingEtterVilkårsvurderingSteg,
                RestTilbakekreving(valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                                   begrunnelse = "Begrunnelse")
        )
        assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterVurderTilbakekrevingSteg.steg)

        leggTilBegrunnelsePåVedtaksperiodeIBehandling(
                behandling = behandlingEtterVurderTilbakekrevingSteg,
                vedtakService = vedtakService,
                vedtaksperiodeService = vedtaksperiodeService,
        )

        val behandlingEtterSendTilBeslutter = stegService.håndterSendTilBeslutter(behandlingEtterVurderTilbakekrevingSteg, "1234")
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
                        task = Task(type = StatusFraOppdragTask.TASK_STEP_TYPE, payload = "")
                ))
        assertEquals(StegType.IVERKSETT_MOT_FAMILIE_TILBAKE, behandlingEtterStatusFraOppdrag.steg)

        val behandlingEtterIverksetteMotTilbake = stegService.håndterIverksettMotFamilieTilbake(behandling, Properties())
        assertEquals(StegType.JOURNALFØR_VEDTAKSBREV, behandlingEtterIverksetteMotTilbake.steg)

        val behandlingEtterJournalførtVedtak =
                stegService.håndterJournalførVedtaksbrev(behandlingEtterIverksetteMotTilbake, JournalførVedtaksbrevDTO(
                        vedtakId = vedtak.id,
                        task = Task(type = JournalførVedtaksbrevTask.TASK_STEP_TYPE, payload = "")
                ))
        assertEquals(StegType.DISTRIBUER_VEDTAKSBREV, behandlingEtterJournalførtVedtak.steg)

        val behandlingEtterDistribuertVedtak = stegService.håndterDistribuerVedtaksbrev(behandlingEtterJournalførtVedtak,
                                                                                        DistribuerDokumentDTO(behandlingId = behandlingEtterJournalførtVedtak.id,
                                                                                                              journalpostId = "1234",
                                                                                                              personIdent = søkerFnr,
                                                                                                              brevmal = hentBrevtype(
                                                                                                                      behandlingEtterJournalførtVedtak),
                                                                                                              erManueltSendt = false))
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
        assertEquals("Behandling med id ${behandling.id} er avsluttet og stegprosessen kan ikke gjenåpnes", feil.message)
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
        val vilkårsvurdertBehandling = kjørGjennomStegInkludertVurderTilbakekreving()

        val henlagtBehandling = stegService.håndterHenleggBehandling(
                vilkårsvurdertBehandling, RestHenleggBehandlingInfo(årsak = HenleggÅrsak.FEILAKTIG_OPPRETTET,
                                                                    begrunnelse = ""))
        assertTrue(henlagtBehandling.behandlingStegTilstand.firstOrNull {
            it.behandlingSteg == StegType.HENLEGG_BEHANDLING && it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
        } != null)
        assertTrue(henlagtBehandling.behandlingStegTilstand.firstOrNull {
            it.behandlingSteg == StegType.FERDIGSTILLE_BEHANDLING && it.behandlingStegStatus == BehandlingStegStatus.IKKE_UTFØRT
        } != null)

        stegService.håndterFerdigstillBehandling(henlagtBehandling)

        val behandlingEtterFerdigstiltBehandling = behandlingService.hent(behandlingId = henlagtBehandling.id)
        assertEquals(StegType.BEHANDLING_AVSLUTTET, behandlingEtterFerdigstiltBehandling.steg)
    }

    @Test
    fun `Henlegge etter behandling er sendt til beslutter`() {
        val vilkårsvurdertBehandling = kjørGjennomStegInkludertVurderTilbakekreving()
        stegService.håndterSendTilBeslutter(vilkårsvurdertBehandling, "1234")

        val behandlingEtterSendTilBeslutter = behandlingService.hent(behandlingId = vilkårsvurdertBehandling.id)

        assertThrows<IllegalStateException> {
            stegService.håndterHenleggBehandling(behandlingEtterSendTilBeslutter,
                                                 RestHenleggBehandlingInfo(årsak = HenleggÅrsak.FEILAKTIG_OPPRETTET,
                                                                           begrunnelse = ""))
        }
    }

    private fun kjørGjennomStegInkludertVurderTilbakekreving(): Behandling {
        val søkerFnr = randomFnr()
        val barnFnr = ClientMocks.barnFnr[0]

        return kjørStegprosessForFGB(
                tilSteg = StegType.VURDER_TILBAKEKREVING,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(barnFnr),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
        )
    }
}