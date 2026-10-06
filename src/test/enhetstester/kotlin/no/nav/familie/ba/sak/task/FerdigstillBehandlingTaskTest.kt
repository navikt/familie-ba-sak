package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.task.dto.FerdigstillBehandlingDTO
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FerdigstillBehandlingTaskTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val stegService = mockk<StegService>()
    private val taskService = mockk<TaskService>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val ferdigstillBehandlingTask =
        FerdigstillBehandlingTask(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            stegService = stegService,
            taskService = taskService,
            featureToggleService = featureToggleService,
        )

    @Nested
    inner class OpprettTask {
        @Test
        fun `skal opprette task med riktig type, payload og properties`() {
            // Arrange
            val personIdent = "12345678901"
            val behandlingsId = 123L

            val forventetDto = FerdigstillBehandlingDTO(behandlingsId, personIdent)

            // Act
            val task = FerdigstillBehandlingTask.opprettTask(personIdent, behandlingsId)

            // Assert
            assertThat(task.type).isEqualTo(FerdigstillBehandlingTask.TASK_STEP_TYPE)
            assertThat(task.payload).isEqualTo(jsonMapper.writeValueAsString(forventetDto))
            assertThat(task.metadata["personIdent"]).isEqualTo(personIdent)
            assertThat(task.metadata["behandlingId"]).isEqualTo(behandlingsId.toString())
        }
    }

    @Nested
    inner class DoTask {
        @Test
        fun `skal opprette task for pensjon når feature toggle er aktivert og behandlingen ferdigstilles`() {
            // Arrange
            val personIdent = "12345678901"
            val behandlingsId = 123L
            val behandling = mockk<Behandling>()

            val task =
                FerdigstillBehandlingTask.opprettTask(
                    søkerIdent = personIdent,
                    behandlingsId = behandlingsId,
                )

            val pensjonTaskSlot = slot<Task>()

            val forventetTask = SendMeldingOmFerdigstiltBehandlingTilPensjonTask.opprettTask(personIdent, behandlingsId)

            every { featureToggleService.isEnabled(FeatureToggle.FERDIGSTILL_BEHANDLING_MELDING_TIL_PENSJON) } returns true
            every { behandlingHentOgPersisterService.hent(behandlingsId) } returns behandling
            every { taskService.save(capture(pensjonTaskSlot)) } returns forventetTask
            every { stegService.håndterFerdigstillBehandling(behandling) } returns behandling

            // Act
            ferdigstillBehandlingTask.doTask(task)

            // Assert
            verify(exactly = 1) { taskService.save(any()) }
            assertThat(pensjonTaskSlot.captured.type).isEqualTo(SendMeldingOmFerdigstiltBehandlingTilPensjonTask.TASK_STEP_TYPE)
            assertThat(pensjonTaskSlot.captured.payload).isEqualTo(forventetTask.payload)
            verify(exactly = 1) { stegService.håndterFerdigstillBehandling(behandling) }
        }

        @Test
        fun `skal ikke opprette task for pensjon når feature toggle er deaktivert, men behandlingen skal fortsatt ferdigstilles`() {
            // Arrange
            val behandlingsId = 456L
            val behandling = mockk<Behandling>()
            val task = FerdigstillBehandlingTask.opprettTask(søkerIdent = "10987654321", behandlingsId = behandlingsId)

            every { featureToggleService.isEnabled(FeatureToggle.FERDIGSTILL_BEHANDLING_MELDING_TIL_PENSJON) } returns false
            every { behandlingHentOgPersisterService.hent(behandlingsId) } returns behandling
            every { stegService.håndterFerdigstillBehandling(behandling) } returns behandling

            // Act
            ferdigstillBehandlingTask.doTask(task)

            // Assert
            verify(exactly = 0) { taskService.save(any()) }
            verify(exactly = 1) { stegService.håndterFerdigstillBehandling(behandling) }
        }
    }
}
