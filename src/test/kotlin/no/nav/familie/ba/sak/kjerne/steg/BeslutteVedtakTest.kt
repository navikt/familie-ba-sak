package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.dokument.DokumentService
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.task.FerdigstillOppgave
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BeslutteVedtakTest {

    private lateinit var beslutteVedtak: BeslutteVedtak
    private lateinit var vedtakService: VedtakService
    private lateinit var behandlingService: BehandlingService
    private lateinit var taskRepository: TaskRepositoryWrapper
    private lateinit var dokumentService: DokumentService
    private lateinit var vilkårsvurderingService: VilkårsvurderingService
    private lateinit var featureToggleService: FeatureToggleService
    private lateinit var økonomiService: ØkonomiService

    val randomVilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())

    @BeforeEach
    fun setUp() {
        val toTrinnKontrollService = mockk<TotrinnskontrollService>()
        vedtakService = mockk()
        taskRepository = mockk()
        dokumentService = mockk()
        behandlingService = mockk()
        vilkårsvurderingService = mockk()
        featureToggleService = mockk()
        økonomiService = mockk()

        val loggService = mockk<LoggService>()

        every { taskRepository.save(any()) } returns Task(OpprettOppgaveTask.TASK_STEP_TYPE, "")
        every {
            toTrinnKontrollService.besluttTotrinnskontroll(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Totrinnskontroll(
            behandling = lagBehandling(), saksbehandler = "Mock Saksbehandler", saksbehandlerId = "Mock.Saksbehandler"
        )
        every { loggService.opprettBeslutningOmVedtakLogg(any(), any(), any()) } just Runs
        every { vedtakService.oppdaterVedtaksdatoOgBrev(any()) } just runs
        every { behandlingService.opprettOgInitierNyttVedtakForBehandling(any(), any(), any()) } just runs
        every { vilkårsvurderingService.hentAktivForBehandling(any()) } returns randomVilkårsvurdering
        every { vilkårsvurderingService.lagreNyOgDeaktiverGammel(any()) } returns randomVilkårsvurdering
        every { featureToggleService.isEnabled(any()) } returns false

        beslutteVedtak = BeslutteVedtak(
            toTrinnKontrollService,
            vedtakService,
            behandlingService,
            taskRepository,
            loggService,
            vilkårsvurderingService,
            featureToggleService,
            økonomiService
        )
    }

    @Test
    fun `Skal ferdigstille Godkjenne vedtak-oppgave ved Godkjent vedtak`() {

        val behandling = lagBehandling()
        behandling.status = BehandlingStatus.FATTER_VEDTAK
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
        val restBeslutningPåVedtak = RestBeslutningPåVedtak(Beslutning.GODKJENT)

        every { vedtakService.hentAktivForBehandling(any()) } returns lagVedtak(behandling)
        mockkObject(FerdigstillOppgave.Companion)
        every { FerdigstillOppgave.opprettTask(any(), any()) } returns Task(FerdigstillOppgave.TASK_STEP_TYPE, "")

        val nesteSteg = beslutteVedtak.utførStegOgAngiNeste(behandling, restBeslutningPåVedtak)

        verify(exactly = 1) { FerdigstillOppgave.opprettTask(behandling.id, Oppgavetype.GodkjenneVedtak) }
        Assertions.assertEquals(StegType.IVERKSETT_MOT_OPPDRAG, nesteSteg)
    }

    @Test
    fun `Skal ferdigstille Godkjenne vedtak-oppgave og opprette Behandle Underkjent Vedtak-oppgave ved Underkjent vedtak`() {
        val behandling = lagBehandling()
        behandling.status = BehandlingStatus.FATTER_VEDTAK
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
        val restBeslutningPåVedtak = RestBeslutningPåVedtak(Beslutning.UNDERKJENT)

        every { vedtakService.hentAktivForBehandling(any()) } returns lagVedtak(behandling)
        mockkObject(FerdigstillOppgave.Companion)
        mockkObject(OpprettOppgaveTask.Companion)
        every { FerdigstillOppgave.opprettTask(any(), any()) } returns Task(FerdigstillOppgave.TASK_STEP_TYPE, "")
        every {
            OpprettOppgaveTask.opprettTask(
                any(),
                any(),
                any(),
                any(), any()
            )
        } returns Task(OpprettOppgaveTask.TASK_STEP_TYPE, "")

        val nesteSteg = beslutteVedtak.utførStegOgAngiNeste(behandling, restBeslutningPåVedtak)

        verify(exactly = 1) { FerdigstillOppgave.opprettTask(behandling.id, Oppgavetype.GodkjenneVedtak) }
        verify(exactly = 1) {
            OpprettOppgaveTask.opprettTask(
                behandling.id,
                Oppgavetype.BehandleUnderkjentVedtak,
                any(),
                any(),
                any()
            )
        }
        Assertions.assertEquals(StegType.SEND_TIL_BESLUTTER, nesteSteg)
    }

    @Test
    fun `Skal initiere nytt vedtak når vedtak ikke er godkjent`() {
        val behandling = lagBehandling()
        behandling.status = BehandlingStatus.FATTER_VEDTAK
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
        val restBeslutningPåVedtak = RestBeslutningPåVedtak(Beslutning.UNDERKJENT)

        every { vedtakService.hentAktivForBehandling(any()) } returns lagVedtak(behandling)
        mockkObject(FerdigstillOppgave.Companion)
        mockkObject(OpprettOppgaveTask.Companion)
        every { FerdigstillOppgave.opprettTask(any(), any()) } returns Task(FerdigstillOppgave.TASK_STEP_TYPE, "")
        every { OpprettOppgaveTask.opprettTask(any(), any(), any()) } returns Task(
            OpprettOppgaveTask.TASK_STEP_TYPE,
            ""
        )

        beslutteVedtak.utførStegOgAngiNeste(behandling, restBeslutningPåVedtak)
        verify(exactly = 1) { behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling, true, emptyList()) }
    }

    @Test
    fun `Skal kaste feil dersom toggle ikke er enabled og årsak er korreksjon vedtaksbrev`() {

        val behandling = lagBehandling(årsak = BehandlingÅrsak.KORREKSJON_VEDTAKSBREV)
        behandling.status = BehandlingStatus.FATTER_VEDTAK
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
        val restBeslutningPåVedtak = RestBeslutningPåVedtak(Beslutning.GODKJENT)

        every { vedtakService.hentAktivForBehandling(any()) } returns lagVedtak(behandling)
        mockkObject(FerdigstillOppgave.Companion)
        every { FerdigstillOppgave.opprettTask(any(), any()) } returns Task(
            type = FerdigstillOppgave.TASK_STEP_TYPE,
            payload = ""
        )

        assertThrows<FunksjonellFeil> { beslutteVedtak.utførStegOgAngiNeste(behandling, restBeslutningPåVedtak) }
    }
}
