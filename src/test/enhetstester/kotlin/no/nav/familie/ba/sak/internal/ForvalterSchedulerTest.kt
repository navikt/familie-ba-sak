package no.nav.familie.ba.sak.internal

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.task.FinnSakerMedFlereMigreringsbehandlingerTask
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.YearMonth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ForvalterSchedulerTest {
    private val taskRepository = mockk<TaskRepositoryWrapper>()
    private val envService = mockk<EnvService>()
    private val service = ForvalterScheduler(taskRepository, envService)
    private val slot = slot<Task>()

    @BeforeEach
    fun initTest() {
        every { envService.erDev() } returns true
        every { taskRepository.save(capture(slot)) } returns Task(type = FinnSakerMedFlereMigreringsbehandlingerTask.TASK_STEP_TYPE, payload = "")
    }

    @AfterAll
    fun clearMocks() {
        clearAllMocks()
    }

    @Test
    fun `Skal opprette task av type finnSakerMedFlereMigreringsbehandlinger`() {
        service.opprettFinnSakerMedFlereMigreringsbehandlingerTask()

        assertThat(slot.captured.payload).isEqualTo(YearMonth.now().minusMonths(1).toString())
        assertThat(slot.captured.type).isEqualTo("finnSakerMedFlereMigreringsbehandlinger")
    }
}
