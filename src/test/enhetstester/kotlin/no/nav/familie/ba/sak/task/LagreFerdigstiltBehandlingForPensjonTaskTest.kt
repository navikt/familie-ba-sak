package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.datagenerator.lagFerdigstiltBehandlingForPensjon
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.ekstern.pensjon.FerdigstiltBehandlingForPensjon
import no.nav.familie.ba.sak.ekstern.pensjon.FerdigstiltBehandlingForPensjonRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.task.dto.LagreFerdigstiltBehandlingForPensjonDTO
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class LagreFerdigstiltBehandlingForPensjonTaskTest {
    private val vedtakService = mockk<VedtakService>()
    private val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    private val repository = mockk<FerdigstiltBehandlingForPensjonRepository>()
    private val taskRepository = mockk<TaskRepositoryWrapper>()
    private val lagreFerdigstiltBehandlingForPensjonTask =
        LagreFerdigstiltBehandlingForPensjonTask(
            vedtakService = vedtakService,
            vedtaksperiodeService = vedtaksperiodeService,
            ferdigstiltBehandlingForPensjonRepository = repository,
            taskRepository = taskRepository,
        )

    @Nested
    inner class OpprettTask {
        @Test
        fun `skal opprette task med riktig type, payload og properties`() {
            // Arrange
            val personIdent = "12345678901"
            val behandlingsId = 123L

            val forventetDto = LagreFerdigstiltBehandlingForPensjonDTO(behandlingsId, personIdent)

            // Act
            val task = LagreFerdigstiltBehandlingForPensjonTask.opprettTask(personIdent, behandlingsId)

            // Assert
            assertThat(task.type).isEqualTo(LagreFerdigstiltBehandlingForPensjonTask.TASK_STEP_TYPE)
            assertThat(task.payload).isEqualTo(jsonMapper.writeValueAsString(forventetDto))
            assertThat(task.metadata["personIdent"]).isEqualTo(personIdent)
            assertThat(task.metadata["behandlingId"]).isEqualTo(behandlingsId.toString())
        }
    }

    @Nested
    inner class DoTask {
        @Test
        fun `skal lagre ferdigstilt behandling med neste sekvensnummer før publiseringstask opprettes`() {
            // Arrange
            val personIdent = "12345678901"
            val behandlingsId = 123L
            val vedtaktidspunkt = LocalDateTime.of(2026, 10, 8, 12, 0)
            val endringstidspunkt = LocalDate.of(2026, 10, 8)

            val task = LagreFerdigstiltBehandlingForPensjonTask.opprettTask(personIdent, behandlingsId)

            val ferdigstiltBehandlingSlot = slot<FerdigstiltBehandlingForPensjon>()
            val publiseringTaskSlot = slot<Task>()

            every { repository.låsTabell() } returns Unit
            every { repository.findByBehandlingId(behandlingsId) } returns null
            every { repository.finnHøyesteSekvensnummer() } returns 7L
            every { vedtakService.hentAktivForBehandlingThrows(behandlingsId) } returns lagVedtak(vedtaksdato = vedtaktidspunkt)
            every { vedtaksperiodeService.finnEndringstidspunktForBehandling(behandlingsId) } returns endringstidspunkt
            every { repository.save(capture(ferdigstiltBehandlingSlot)) } answers { firstArg() }
            every { taskRepository.save(capture(publiseringTaskSlot)) } answers { firstArg() }

            // Act
            lagreFerdigstiltBehandlingForPensjonTask.doTask(task)

            // Assert
            assertThat(ferdigstiltBehandlingSlot.captured.sekvensnummer).isEqualTo(8L)
            assertThat(ferdigstiltBehandlingSlot.captured.behandlingId).isEqualTo(behandlingsId)
            assertThat(ferdigstiltBehandlingSlot.captured.ident).isEqualTo(personIdent)
            assertThat(ferdigstiltBehandlingSlot.captured.vedtaktidspunkt).isEqualTo(vedtaktidspunkt)
            assertThat(ferdigstiltBehandlingSlot.captured.endringstidspunkt).isEqualTo(endringstidspunkt)
            assertThat(publiseringTaskSlot.captured.type).isEqualTo(PubliserFerdigstiltBehandlingForPensjonTask.TASK_STEP_TYPE)
            assertThat(publiseringTaskSlot.captured.payload).isEqualTo("8")
            verify(exactly = 1) { vedtakService.hentAktivForBehandlingThrows(behandlingsId) }
            verify(exactly = 1) { vedtaksperiodeService.finnEndringstidspunktForBehandling(behandlingsId) }
            verifyOrder {
                repository.låsTabell()
                repository.findByBehandlingId(behandlingsId)
                repository.finnHøyesteSekvensnummer()
                repository.save(any())
                taskRepository.save(any())
            }
        }

        @Test
        fun `skal kaste feil dersom ferdigstilt behandling allerede er lagret`() {
            // Arrange
            val ferdigstiltBehandling = lagFerdigstiltBehandlingForPensjon(sekvensnummer = 5L)
            val task = LagreFerdigstiltBehandlingForPensjonTask.opprettTask(ferdigstiltBehandling.ident, ferdigstiltBehandling.behandlingId)
            every { repository.låsTabell() } returns Unit
            every { repository.findByBehandlingId(ferdigstiltBehandling.behandlingId) } returns ferdigstiltBehandling

            // Act & Assert
            val feil = assertThrows<Feil> { lagreFerdigstiltBehandlingForPensjonTask.doTask(task) }

            assertThat(feil.message).isEqualTo(
                "Ferdigstilt behandling for pensjon er allerede lagret for behandling=${ferdigstiltBehandling.behandlingId} med sekvensnummer=5",
            )
            verify(exactly = 0) { repository.finnHøyesteSekvensnummer() }
            verify(exactly = 0) { repository.save(any()) }
            verify(exactly = 0) { taskRepository.save(any()) }
            verify(exactly = 0) { vedtakService.hentAktivForBehandlingThrows(any()) }
        }

        @Test
        fun `skal kaste feil dersom vedtaket mangler vedtaksdato`() {
            // Arrange
            val behandlingsId = 123L
            val task = LagreFerdigstiltBehandlingForPensjonTask.opprettTask("12345678901", behandlingsId)

            every { repository.låsTabell() } returns Unit
            every { repository.findByBehandlingId(behandlingsId) } returns null
            every { vedtakService.hentAktivForBehandlingThrows(behandlingsId) } returns lagVedtak(vedtaksdato = null)

            // Act & Assert
            val feil = assertThrows<Feil> { lagreFerdigstiltBehandlingForPensjonTask.doTask(task) }

            assertThat(feil.message).isEqualTo("Fant ikke vedtaksdato for vedtak for behandling=$behandlingsId")
            verify(exactly = 0) { repository.save(any()) }
            verify(exactly = 0) { taskRepository.save(any()) }
        }

        @Test
        fun `skal ikke opprette publiseringstask når lagring feiler`() {
            // Arrange
            val ferdigstiltBehandling = lagFerdigstiltBehandlingForPensjon()
            val task = LagreFerdigstiltBehandlingForPensjonTask.opprettTask(ferdigstiltBehandling.ident, ferdigstiltBehandling.behandlingId)
            every { repository.låsTabell() } returns Unit
            every { repository.findByBehandlingId(ferdigstiltBehandling.behandlingId) } returns null
            every { repository.finnHøyesteSekvensnummer() } returns 0L
            every { vedtakService.hentAktivForBehandlingThrows(ferdigstiltBehandling.behandlingId) } returns lagVedtak(vedtaksdato = ferdigstiltBehandling.vedtaktidspunkt)
            every { vedtaksperiodeService.finnEndringstidspunktForBehandling(ferdigstiltBehandling.behandlingId) } returns ferdigstiltBehandling.endringstidspunkt
            every { repository.save(any()) } throws IllegalStateException("Lagring feilet")

            // Act & Assert
            assertThrows<IllegalStateException> { lagreFerdigstiltBehandlingForPensjonTask.doTask(task) }

            verify(exactly = 0) { taskRepository.save(any()) }
        }
    }
}
