package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.task.dto.FerdigstillBehandlingDTO
import no.nav.familie.kontrakter.felles.jsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FerdigstillBehandlingTaskTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val stegService = mockk<StegService>()
    private val ferdigstillBehandlingTask =
        FerdigstillBehandlingTask(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            stegService = stegService,
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
        fun `skal ferdigstille behandlingen`() {
            // Arrange
            val behandlingsId = 123L
            val behandling = mockk<Behandling>()
            val task = FerdigstillBehandlingTask.opprettTask(søkerIdent = "12345678901", behandlingsId = behandlingsId)

            every { behandlingHentOgPersisterService.hent(behandlingsId) } returns behandling
            every { stegService.håndterFerdigstillBehandling(behandling) } returns behandling

            // Act
            ferdigstillBehandlingTask.doTask(task)

            // Assert
            verify(exactly = 1) { stegService.håndterFerdigstillBehandling(behandling) }
        }
    }
}
