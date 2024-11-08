package no.nav.familie.ba.sak.internal

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth

class ForvalterControllerTest {
    val tilgangService: TilgangService = mockk()
    val taskService: TaskService = mockk()
    val forvalterController =
        ForvalterController(
            oppgaveRepository = mockk(),
            integrasjonClient = mockk(),
            restartAvSmåbarnstilleggService = mockk(),
            forvalterService = mockk(),
            ecbService = mockk(),
            testVerktøyService = mockk(),
            tilgangService = tilgangService,
            økonomiService = mockk(),
            opprettTaskService = mockk(),
            taskService = taskService,
            satskjøringRepository = mockk(),
            autovedtakMånedligValutajusteringService = mockk(),
            månedligValutajusteringScheduler = mockk(),
            fagsakService = mockk(),
            unleashNextMedContextService = mockk(),
            taskRepository = mockk(),
            behandlingHentOgPersisterService = mockk(),
            stønadsstatistikkService = mockk(),
            persongrunnlagService = mockk(),
        )

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.verifiserHarTilgangTilHandling(any(), any()) }
    }

    @Test
    fun oppdaterValutakurs() {
        val behandling = lagBehandling()
        val endringstidspunkt = YearMonth.of(2024, 1)
        val task = slot<Task>()

        every { taskService.save(capture(task)) } returns Task(type = "", payload = "")

        forvalterController.oppdaterValutakurs(behandlingId = behandling.id, endringstidspunkt = endringstidspunkt)

        assertThat(task.captured.type).isEqualTo("oppdaterValutakurs")
        assertThat(task.captured.payload).isEqualTo("{\"behandlingId\":${behandling.id},\"endringstidspunkt\":\"$endringstidspunkt\"}")
    }
}
