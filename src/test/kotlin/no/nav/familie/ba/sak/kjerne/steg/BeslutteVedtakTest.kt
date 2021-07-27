package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.*
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.kjerne.dokument.DokumentService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.task.FerdigstillOppgave
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BeslutteVedtakTest {

    private lateinit var beslutteVedtak: BeslutteVedtak
    private lateinit var vedtakService: VedtakService
    private lateinit var behandlingService: BehandlingService
    private lateinit var taskRepository: TaskRepository
    private lateinit var dokumentService: DokumentService
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    val randomVilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())

    @BeforeEach
    fun setUp() {
        val toTrinnKontrollService = mockk<TotrinnskontrollService>()
        vedtakService = mockk()
        taskRepository = mockk()
        dokumentService = mockk()
        behandlingService = mockk()
        vilkårsvurderingService = mockk()
        val loggService = mockk<LoggService>()

        every { taskRepository.save(any()) } returns Task.nyTask(OpprettOppgaveTask.TASK_STEP_TYPE, "")
        every { toTrinnKontrollService.besluttTotrinnskontroll(any(), any(), any(), any()) } just Runs
        every { loggService.opprettBeslutningOmVedtakLogg(any(), any(), any()) } just Runs
        every { vedtakService.oppdaterVedtaksdatoOgBrev(any()) } just runs
        every { behandlingService.opprettOgInitierNyttVedtakForBehandling(any(), any(), any()) } just runs
        every { vilkårsvurderingService.hentAktivForBehandling(any()) } returns randomVilkårsvurdering
        every { vilkårsvurderingService.lagreNyOgDeaktiverGammel(any()) } returns randomVilkårsvurdering

        beslutteVedtak = BeslutteVedtak(toTrinnKontrollService,
                                        vedtakService,
                                        behandlingService,
                                        taskRepository,
                                        loggService,
                                        vilkårsvurderingService)
    }

    @Test
    fun `Skal ferdigstille Godkjenne vedtak-oppgave ved Godkjent vedtak`() {

        val behandling = lagBehandling()
        behandling.status = BehandlingStatus.FATTER_VEDTAK
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
        val restBeslutningPåVedtak = RestBeslutningPåVedtak(Beslutning.GODKJENT)

        every { vedtakService.hentAktivForBehandling(any()) } returns lagVedtak(behandling)
        mockkObject(FerdigstillOppgave.Companion)
        every { FerdigstillOppgave.opprettTask(any(), any()) } returns Task.nyTask(FerdigstillOppgave.TASK_STEP_TYPE, "")

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
        every { FerdigstillOppgave.opprettTask(any(), any()) } returns Task.nyTask(FerdigstillOppgave.TASK_STEP_TYPE, "")
        every { OpprettOppgaveTask.opprettTask(any(), any(), any()) } returns Task.nyTask(OpprettOppgaveTask.TASK_STEP_TYPE, "")

        val nesteSteg = beslutteVedtak.utførStegOgAngiNeste(behandling, restBeslutningPåVedtak)

        verify(exactly = 1) { FerdigstillOppgave.opprettTask(behandling.id, Oppgavetype.GodkjenneVedtak) }
        verify(exactly = 1) { OpprettOppgaveTask.opprettTask(behandling.id, Oppgavetype.BehandleUnderkjentVedtak, any()) }
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
        every { FerdigstillOppgave.opprettTask(any(), any()) } returns Task.nyTask(FerdigstillOppgave.TASK_STEP_TYPE, "")
        every { OpprettOppgaveTask.opprettTask(any(), any(), any()) } returns Task.nyTask(OpprettOppgaveTask.TASK_STEP_TYPE, "")

        beslutteVedtak.utførStegOgAngiNeste(behandling, restBeslutningPåVedtak)
        verify(exactly = 1) { behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling, true, emptyList()) }
    }
}