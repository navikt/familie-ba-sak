package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.ba.sak.task.dto.OpprettOppgaveTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OpprettOppgaveTaskTest {
    private val oppgaveService = mockk<OppgaveService>(relaxed = true)
    private val hentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val opprettOppgaveTask = OpprettOppgaveTask(oppgaveService, hentOgPersisterService)

    @Test
    fun `oppretter oppgave når behandling har annen status enn iverksatt eller avsluttet`() {
        // Given
        val behandling =
            mockk<Behandling> {
                every { status } returns BehandlingStatus.UTREDES
                every { id } returns 1L
            }
        val dto =
            OpprettOppgaveTaskDTO(
                behandlingId = 1L,
                oppgavetype = Oppgavetype.BehandleSak,
                fristForFerdigstillelse = LocalDate.now(),
                tilordnetRessurs = "Z12345",
                beskrivelse = "Testoppgave",
                manuellOppgaveType = null,
            )
        val task = Task(type = OpprettOppgaveTask.TASK_STEP_TYPE, payload = objectMapper.writeValueAsString(dto))

        every { hentOgPersisterService.hent(1L) } returns behandling
        every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), any()) } returns "12345"

        // When
        opprettOppgaveTask.doTask(task)

        // Then
        verify(exactly = 1) {
            oppgaveService.opprettOppgave(
                behandlingId = 1L,
                oppgavetype = Oppgavetype.BehandleSak,
                fristForFerdigstillelse = dto.fristForFerdigstillelse,
                tilordnetNavIdent = "Z12345",
                beskrivelse = "Testoppgave",
                manuellOppgaveType = null,
            )
        }
        assertEquals("12345", task.metadata["oppgaveId"])
    }

    @Test
    fun `oppretter ikke oppgave når behandling er iverksatt og oppgavetype er GodkjenneVedtak`() {
        // Given
        val behandling =
            mockk<Behandling> {
                every { status } returns BehandlingStatus.IVERKSETTER_VEDTAK
                every { id } returns 2L
            }
        val dto =
            OpprettOppgaveTaskDTO(
                behandlingId = 2L,
                oppgavetype = Oppgavetype.GodkjenneVedtak,
                fristForFerdigstillelse = LocalDate.now(),
                tilordnetRessurs = null,
                beskrivelse = null,
                manuellOppgaveType = null,
            )
        val task = Task(type = OpprettOppgaveTask.TASK_STEP_TYPE, payload = objectMapper.writeValueAsString(dto))

        every { hentOgPersisterService.hent(2L) } returns behandling

        // When
        opprettOppgaveTask.doTask(task)

        // Then
        verify(exactly = 0) { oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `oppretter ikke oppgave når behandling er avsluttet og oppgavetype er BehandleUnderkjentVedtak`() {
        // Given
        val behandling =
            mockk<Behandling> {
                every { status } returns BehandlingStatus.AVSLUTTET
                every { id } returns 3L
            }
        val dto =
            OpprettOppgaveTaskDTO(
                behandlingId = 3L,
                oppgavetype = Oppgavetype.BehandleUnderkjentVedtak,
                fristForFerdigstillelse = LocalDate.now(),
                tilordnetRessurs = null,
                beskrivelse = null,
                manuellOppgaveType = null,
            )
        val task = Task(type = OpprettOppgaveTask.TASK_STEP_TYPE, payload = objectMapper.writeValueAsString(dto))

        every { hentOgPersisterService.hent(3L) } returns behandling

        // When
        opprettOppgaveTask.doTask(task)

        // Then
        verify(exactly = 0) { oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `oppretter oppgave når behandling er iverksatt men oppgavetype er verken GodkjenneVedtak eller BehandleUnderkjentVedtak`() {
        // Given
        val behandling =
            mockk<Behandling> {
                every { status } returns BehandlingStatus.IVERKSETTER_VEDTAK
                every { id } returns 4L
            }
        val dto =
            OpprettOppgaveTaskDTO(
                behandlingId = 4L,
                oppgavetype = Oppgavetype.VurderLivshendelse,
                fristForFerdigstillelse = LocalDate.now(),
                tilordnetRessurs = null,
                beskrivelse = null,
                manuellOppgaveType = null,
            )
        val task = Task(type = OpprettOppgaveTask.TASK_STEP_TYPE, payload = objectMapper.writeValueAsString(dto))

        every { hentOgPersisterService.hent(4L) } returns behandling
        every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), any()) } returns "67890"

        // When
        opprettOppgaveTask.doTask(task)

        // Then
        verify(exactly = 1) { oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `oppretter oppgave med alle parametere fylt ut`() {
        // Given
        val behandling =
            mockk<Behandling> {
                every { status } returns BehandlingStatus.UTREDES
                every { id } returns 5L
            }
        val dto =
            OpprettOppgaveTaskDTO(
                behandlingId = 5L,
                oppgavetype = Oppgavetype.BehandleSak,
                fristForFerdigstillelse = LocalDate.now().plusDays(7),
                tilordnetRessurs = "Z99999",
                beskrivelse = "Detaljert beskrivelse",
                manuellOppgaveType = ManuellOppgaveType.ÅPEN_BEHANDLING,
            )
        val task = Task(type = OpprettOppgaveTask.TASK_STEP_TYPE, payload = objectMapper.writeValueAsString(dto))

        every { hentOgPersisterService.hent(5L) } returns behandling
        every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), any()) } returns "11111"

        // When
        opprettOppgaveTask.doTask(task)

        // Then
        verify(exactly = 1) {
            oppgaveService.opprettOppgave(
                behandlingId = 5L,
                oppgavetype = Oppgavetype.BehandleSak,
                fristForFerdigstillelse = dto.fristForFerdigstillelse,
                tilordnetNavIdent = "Z99999",
                beskrivelse = "Detaljert beskrivelse",
                manuellOppgaveType = ManuellOppgaveType.ÅPEN_BEHANDLING,
            )
        }
    }
}
