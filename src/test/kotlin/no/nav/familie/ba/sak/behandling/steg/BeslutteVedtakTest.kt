package no.nav.familie.ba.sak.behandling.steg

import io.mockk.*
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.vedtak.Beslutning
import no.nav.familie.ba.sak.behandling.vedtak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.dokument.DokumentService
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.task.FerdigstillOppgave
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BeslutteVedtakTest {

    private lateinit var beslutteVedtak: BeslutteVedtak
    private lateinit var vedtakService: VedtakService
    private lateinit var taskRepository: TaskRepository
    private lateinit var dokumentService: DokumentService

    @BeforeEach
    fun setUp() {
        val toTrinnKontrollService = mockk<TotrinnskontrollService>()
        vedtakService = mockk()
        taskRepository = mockk()
        dokumentService = mockk()
        val loggService = mockk<LoggService>()

        every { taskRepository.save(any()) } returns Task.nyTask(OpprettOppgaveTask.TASK_STEP_TYPE, "")
        every { toTrinnKontrollService.besluttTotrinnskontroll(any(), any(), any()) } just Runs
        every { loggService.opprettBeslutningOmVedtakLogg(any(), any(), any()) } just Runs
        every { vedtakService.besluttVedtak(any()) } just runs

        beslutteVedtak = BeslutteVedtak(toTrinnKontrollService, vedtakService, taskRepository, loggService)
    }

    @Test
    fun `Skal ferdigstille Godkjenne vedtak-oppgave ved Godkjent vedtak`() {

        val behandling = lagBehandling()
        behandling.status = BehandlingStatus.SENDT_TIL_BESLUTTER
        behandling.steg = StegType.BESLUTTE_VEDTAK
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
        behandling.status = BehandlingStatus.SENDT_TIL_BESLUTTER
        behandling.steg = StegType.BESLUTTE_VEDTAK
        val restBeslutningPåVedtak = RestBeslutningPåVedtak(Beslutning.UNDERKJENT)

        every { vedtakService.hentAktivForBehandling(any()) } returns lagVedtak(behandling)
        mockkObject(FerdigstillOppgave.Companion)
        mockkObject(OpprettOppgaveTask.Companion)
        every { FerdigstillOppgave.opprettTask(any(), any()) } returns Task.nyTask(FerdigstillOppgave.TASK_STEP_TYPE, "")
        every { OpprettOppgaveTask.opprettTask(any(), any(), any()) } returns Task.nyTask(OpprettOppgaveTask.TASK_STEP_TYPE, "")

        val nesteSteg = beslutteVedtak.utførStegOgAngiNeste(behandling, restBeslutningPåVedtak)

        verify(exactly = 1) { FerdigstillOppgave.opprettTask(behandling.id, Oppgavetype.GodkjenneVedtak) }
        verify(exactly = 1) { OpprettOppgaveTask.opprettTask(behandling.id, Oppgavetype.BehandleUnderkjentVedtak, any()) }
        Assertions.assertEquals(StegType.REGISTRERE_SØKNAD, nesteSteg)
    }
}