package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFerdigstiltBehandlingForPensjon
import no.nav.familie.ba.sak.ekstern.pensjon.FerdigstiltBehandlingForPensjon
import no.nav.familie.ba.sak.ekstern.pensjon.FerdigstiltBehandlingForPensjonRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
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
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val lagreFerdigstiltBehandlingForPensjonTask =
        LagreFerdigstiltBehandlingForPensjonTask(
            vedtakService = vedtakService,
            vedtaksperiodeService = vedtaksperiodeService,
            ferdigstiltBehandlingForPensjonRepository = repository,
            taskRepository = taskRepository,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        )

    @Nested
    inner class OpprettTask {
        @Test
        fun `skal opprette task med riktig type, payload og properties`() {
            // Arrange
            val personIdent = "12345678901"
            val behandlingsId = 123L
            val opprettetDato = LocalDate.of(2026, 10, 29)

            val forventetDto = LagreFerdigstiltBehandlingForPensjonDTO(behandlingsId, personIdent, opprettetDato)

            // Act
            val task = LagreFerdigstiltBehandlingForPensjonTask.opprettTask(personIdent, behandlingsId, opprettetDato)

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
            val vedtakstidspunkt = LocalDateTime.of(2026, 10, 8, 12, 0)
            val endringstidspunkt = LocalDate.of(2026, 10, 8)

            val task = LagreFerdigstiltBehandlingForPensjonTask.opprettTask(personIdent, behandlingsId)

            val ferdigstiltBehandlingSlot = slot<FerdigstiltBehandlingForPensjon>()
            val publiseringTaskSlot = slot<Task>()

            every { repository.låsTabell() } returns Unit
            every { repository.findByBehandlingId(behandlingsId) } returns null
            every { repository.finnHøyesteSekvensnummer() } returns 7L
            every { vedtakService.hentVedtaksdatoForBehandlingThrows(behandlingsId) } returns vedtakstidspunkt
            every { behandlingHentOgPersisterService.hent(behandlingsId) } returns lagBehandling(id = behandlingsId)
            every { vedtaksperiodeService.finnEndringstidspunktForBehandling(behandlingsId) } returns endringstidspunkt
            every { repository.save(capture(ferdigstiltBehandlingSlot)) } answers { firstArg() }
            every { taskRepository.save(capture(publiseringTaskSlot)) } answers { firstArg() }

            // Act
            lagreFerdigstiltBehandlingForPensjonTask.doTask(task)

            // Assert
            assertThat(ferdigstiltBehandlingSlot.captured.sekvensnummer).isEqualTo(8L)
            assertThat(ferdigstiltBehandlingSlot.captured.behandlingId).isEqualTo(behandlingsId)
            assertThat(ferdigstiltBehandlingSlot.captured.ident).isEqualTo(personIdent)
            assertThat(ferdigstiltBehandlingSlot.captured.vedtakstidspunkt).isEqualTo(vedtakstidspunkt)
            assertThat(ferdigstiltBehandlingSlot.captured.endringstidspunkt).isEqualTo(endringstidspunkt)
            assertThat(publiseringTaskSlot.captured.type).isEqualTo(PubliserFerdigstiltBehandlingForPensjonTask.TASK_STEP_TYPE)
            assertThat(publiseringTaskSlot.captured.payload).isEqualTo("8")
            verify(exactly = 1) { vedtakService.hentVedtaksdatoForBehandlingThrows(behandlingsId) }
            verify(exactly = 1) { vedtaksperiodeService.finnEndringstidspunktForBehandling(behandlingsId) }
            verifyOrder {
                vedtakService.hentVedtaksdatoForBehandlingThrows(behandlingsId)
                vedtaksperiodeService.finnEndringstidspunktForBehandling(behandlingsId)
                repository.låsTabell()
                repository.findByBehandlingId(behandlingsId)
                repository.finnHøyesteSekvensnummer()
                repository.save(any())
                taskRepository.save(any())
            }
        }

        @Test
        fun `skal bruke overstyrt endringstidspunkt når det finnes`() {
            // Arrange
            val ferdigstiltBehandling = lagFerdigstiltBehandlingForPensjon()
            val overstyrtEndringstidspunkt = LocalDate.of(2026, 9, 1)
            val behandling =
                lagBehandling(id = ferdigstiltBehandling.behandlingId).also {
                    it.overstyrtEndringstidspunkt = overstyrtEndringstidspunkt
                }
            val task = LagreFerdigstiltBehandlingForPensjonTask.opprettTask(ferdigstiltBehandling.ident, behandling.id)
            val ferdigstiltBehandlingSlot = slot<FerdigstiltBehandlingForPensjon>()

            every { repository.låsTabell() } returns Unit
            every { repository.findByBehandlingId(behandling.id) } returns null
            every { repository.finnHøyesteSekvensnummer() } returns 0L
            every { vedtakService.hentVedtaksdatoForBehandlingThrows(behandling.id) } returns ferdigstiltBehandling.vedtakstidspunkt
            every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling
            every { repository.save(capture(ferdigstiltBehandlingSlot)) } answers { firstArg() }
            every { taskRepository.save(any()) } answers { firstArg() }

            // Act
            lagreFerdigstiltBehandlingForPensjonTask.doTask(task)

            // Assert
            assertThat(ferdigstiltBehandlingSlot.captured.endringstidspunkt).isEqualTo(overstyrtEndringstidspunkt)
            verify(exactly = 0) { vedtaksperiodeService.finnEndringstidspunktForBehandling(any()) }
        }

        @Test
        fun `skal bruke opprettetDato fra task som endringstidspunkt når endringstidspunkt er TIDENES_ENDE`() {
            // Arrange
            val ferdigstiltBehandling = lagFerdigstiltBehandlingForPensjon()
            val behandling = lagBehandling(id = ferdigstiltBehandling.behandlingId)
            val opprettetDato = LocalDate.of(2026, 10, 1)
            val task = LagreFerdigstiltBehandlingForPensjonTask.opprettTask(ferdigstiltBehandling.ident, behandling.id, opprettetDato)
            val ferdigstiltBehandlingSlot = slot<FerdigstiltBehandlingForPensjon>()

            every { repository.låsTabell() } returns Unit
            every { repository.findByBehandlingId(behandling.id) } returns null
            every { repository.finnHøyesteSekvensnummer() } returns 0L
            every { vedtakService.hentVedtaksdatoForBehandlingThrows(behandling.id) } returns ferdigstiltBehandling.vedtakstidspunkt
            every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling
            every { vedtaksperiodeService.finnEndringstidspunktForBehandling(behandling.id) } returns TIDENES_ENDE
            every { repository.save(capture(ferdigstiltBehandlingSlot)) } answers { firstArg() }
            every { taskRepository.save(any()) } answers { firstArg() }

            // Act
            lagreFerdigstiltBehandlingForPensjonTask.doTask(task)

            // Assert
            assertThat(ferdigstiltBehandlingSlot.captured.endringstidspunkt).isEqualTo(opprettetDato)
        }

        @Test
        fun `skal kaste feil dersom ferdigstilt behandling allerede er lagret`() {
            // Arrange
            val ferdigstiltBehandling = lagFerdigstiltBehandlingForPensjon(sekvensnummer = 5L)
            val task = LagreFerdigstiltBehandlingForPensjonTask.opprettTask(ferdigstiltBehandling.ident, ferdigstiltBehandling.behandlingId)
            every { behandlingHentOgPersisterService.hent(ferdigstiltBehandling.behandlingId) } returns lagBehandling(id = ferdigstiltBehandling.behandlingId)
            every { vedtakService.hentVedtaksdatoForBehandlingThrows(ferdigstiltBehandling.behandlingId) } returns ferdigstiltBehandling.vedtakstidspunkt
            every { vedtaksperiodeService.finnEndringstidspunktForBehandling(ferdigstiltBehandling.behandlingId) } returns ferdigstiltBehandling.endringstidspunkt
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
        }

        @Test
        fun `skal kaste feil dersom vedtaket mangler vedtaksdato`() {
            // Arrange
            val behandlingsId = 123L
            val task = LagreFerdigstiltBehandlingForPensjonTask.opprettTask("12345678901", behandlingsId)

            every { behandlingHentOgPersisterService.hent(behandlingsId) } returns lagBehandling(id = behandlingsId)
            every { vedtakService.hentVedtaksdatoForBehandlingThrows(behandlingsId) } throws Feil("Vedtaksdato mangler")

            // Act & Assert
            val feil = assertThrows<Feil> { lagreFerdigstiltBehandlingForPensjonTask.doTask(task) }

            assertThat(feil.message).isEqualTo("Vedtaksdato mangler")
            verify(exactly = 0) { repository.låsTabell() }
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
            every { vedtakService.hentVedtaksdatoForBehandlingThrows(ferdigstiltBehandling.behandlingId) } returns ferdigstiltBehandling.vedtakstidspunkt
            every { behandlingHentOgPersisterService.hent(ferdigstiltBehandling.behandlingId) } returns lagBehandling(id = ferdigstiltBehandling.behandlingId)
            every { vedtaksperiodeService.finnEndringstidspunktForBehandling(ferdigstiltBehandling.behandlingId) } returns ferdigstiltBehandling.endringstidspunkt
            every { repository.save(any()) } throws IllegalStateException("Lagring feilet")

            // Act & Assert
            assertThrows<IllegalStateException> { lagreFerdigstiltBehandlingForPensjonTask.doTask(task) }

            verify(exactly = 0) { taskRepository.save(any()) }
        }
    }
}
