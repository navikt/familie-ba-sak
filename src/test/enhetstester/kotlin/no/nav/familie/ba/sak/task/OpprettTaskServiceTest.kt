package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.domene.SatsendringEøsKjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.domene.SatsendringEøsKjøringRepository
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth

class OpprettTaskServiceTest {
    private val taskRepository = mockk<TaskRepositoryWrapper>()
    private val satskjøringRepository = mockk<SatskjøringRepository>()
    private val satsendringEøsKjøringRepository = mockk<SatsendringEøsKjøringRepository>()
    private val envService = mockk<EnvService>()

    private val opprettTaskService =
        OpprettTaskService(
            taskRepository = taskRepository,
            satskjøringRepository = satskjøringRepository,
            satsendringEøsKjøringRepository = satsendringEøsKjøringRepository,
            envService = envService,
        )

    @Nested
    inner class OpprettSatsendringEøsTask {
        private val fagsakId = 1L
        private val utbetalingsland = "PL"
        private val satsTidspunkt = YearMonth.of(2026, 1)

        @BeforeEach
        fun setUp() {
            every {
                satsendringEøsKjøringRepository.findByFagsakIdAndUtbetalingslandAndSatsTidspunkt(fagsakId, utbetalingsland, satsTidspunkt)
            } returns null
            every { satsendringEøsKjøringRepository.save(any()) } answers { firstArg() }
            every { taskRepository.save(any()) } answers { firstArg() }
        }

        @Test
        fun `opprettSatsendringEøsTask oppretter SatsendringEøsKjøring når ingen finnes fra før`() {
            // Act
            opprettTaskService.opprettSatsendringEøsTask(fagsakId, utbetalingsland, satsTidspunkt)

            // Assert
            val kjøringSlot = slot<SatsendringEøsKjøring>()
            verify(exactly = 1) { satsendringEøsKjøringRepository.save(capture(kjøringSlot)) }
            assertThat(kjøringSlot.captured.fagsakId).isEqualTo(fagsakId)
            assertThat(kjøringSlot.captured.utbetalingsland).isEqualTo(utbetalingsland)
            assertThat(kjøringSlot.captured.satsTidspunkt).isEqualTo(satsTidspunkt)
        }

        @Test
        fun `opprettSatsendringEøsTask oppretter ikke ny SatsendringEøsKjøring når en allerede finnes`() {
            // Arrange
            every {
                satsendringEøsKjøringRepository.findByFagsakIdAndUtbetalingslandAndSatsTidspunkt(fagsakId, utbetalingsland, satsTidspunkt)
            } returns
                SatsendringEøsKjøring(fagsakId = fagsakId, utbetalingsland = utbetalingsland, satsTidspunkt = satsTidspunkt)

            // Act
            opprettTaskService.opprettSatsendringEøsTask(fagsakId, utbetalingsland, satsTidspunkt)

            // Assert
            verify(exactly = 0) { satsendringEøsKjøringRepository.save(any()) }
        }

        @Test
        fun `opprettSatsendringEøsTask oppretter task med riktig type, payload og properties`() {
            // Arrange
            val taskSlot = slot<Task>()

            // Act
            opprettTaskService.opprettSatsendringEøsTask(fagsakId, utbetalingsland, satsTidspunkt)

            // Assert
            verify(exactly = 1) { taskRepository.save(capture(taskSlot)) }
            assertThat(taskSlot.captured.type).isEqualTo(SatsendringEøsTask.TASK_STEP_TYPE)
            assertThat(taskSlot.captured.payload).isEqualTo(jsonMapper.writeValueAsString(SatsendringEøsTaskDto(fagsakId, utbetalingsland, satsTidspunkt)))
            assertThat(taskSlot.captured.metadata["fagsakId"]).isEqualTo(fagsakId.toString())
            assertThat(taskSlot.captured.metadata["utbetalingsland"]).isEqualTo(utbetalingsland)
            assertThat(taskSlot.captured.metadata["satsTidspunkt"]).isEqualTo(satsTidspunkt.toString())
        }
    }
}
