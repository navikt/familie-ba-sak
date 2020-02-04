package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import junit.framework.Assert.assertEquals
import no.nav.familie.ba.sak.økonomi.AvstemmingService
import no.nav.familie.ba.sak.økonomi.AvstemmingTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvstemMotOppdragTest {

    lateinit var avstemMotOppdrag: AvstemMotOppdrag
    lateinit var taskRepositoryMock: TaskRepository

    @BeforeEach
    fun setUp() {
        val avstemmingServiceMock = mockk<AvstemmingService>()
        taskRepositoryMock = mockk()
        avstemMotOppdrag = AvstemMotOppdrag(avstemmingServiceMock, taskRepositoryMock)
    }

    @Test
    fun skalBeregneNesteAvstemmingForHelg() {
        val enLørdag = LocalDate.of(2020, 1, 11)

        val testDto = avstemMotOppdrag.nesteAvstemmingDTO(enLørdag, 1)

        assertEquals(LocalDate.of(2020, 1, 13).atStartOfDay(), testDto.tomDato)
        assertEquals(LocalDate.of(2020, 1, 10).atStartOfDay(), testDto.fomDato)
    }

    @Test
    fun skalBeregneNesteAvstemmingForSammenhengendeHelligdag() {
        val førsteJuledag = LocalDate.of(2019, 12, 25)

        val testDto = avstemMotOppdrag.nesteAvstemmingDTO(førsteJuledag, 1)

        assertEquals(LocalDate.of(2019, 12, 27).atStartOfDay(), testDto.tomDato)
        assertEquals(LocalDate.of(2019, 12, 24).atStartOfDay(), testDto.fomDato)
    }

    @Test
    fun skalBeregneNesteAvstemmingForEnkeltHelligdag() {
        val førsteNyttårsdag = LocalDate.of(2020, 1, 1)

        val testDto = avstemMotOppdrag.nesteAvstemmingDTO(førsteNyttårsdag, 1)

        assertEquals(LocalDate.of(2020, 1, 2).atStartOfDay(), testDto.tomDato)
        assertEquals(LocalDate.of(2019, 12, 31).atStartOfDay(), testDto.fomDato)
    }

    @Test
    fun skalBeregneNesteAvstemmingForLanghelg() {
        val fredagFørsteMai = LocalDate.of(2020, 5, 1)

        val testDto = avstemMotOppdrag.nesteAvstemmingDTO(fredagFørsteMai, 1)

        assertEquals(LocalDate.of(2020, 5, 4).atStartOfDay(), testDto.tomDato)
        assertEquals(LocalDate.of(2020, 4, 30).atStartOfDay(), testDto.fomDato)
    }

    @Test
    fun skalBeregneNesteAvstemmingForUkedag() {
        val enOnsdag = LocalDate.of(2020, 1, 15)

        val testDto = avstemMotOppdrag.nesteAvstemmingDTO(enOnsdag, 1)

        assertEquals(LocalDate.of(2020, 1, 15).atStartOfDay(), testDto.tomDato)
        assertEquals(LocalDate.of(2020, 1, 14).atStartOfDay(), testDto.fomDato)
    }

    @Test
    fun skalLageNyAvstemmingstaskEtterJobb() {
        val iDag = LocalDate.of(2020, 1, 15).atStartOfDay()
        val testTask = Task.nyTaskMedTriggerTid(AvstemMotOppdrag.TASK_STEP_TYPE,
                                                objectMapper.writeValueAsString(AvstemmingTaskDTO(iDag.minusDays(1), iDag)),
                                                iDag.toLocalDate().atTime(8, 0))
        val slot = slot<Task>()
        every { taskRepositoryMock.save(any()) } returns testTask

        avstemMotOppdrag.onCompletion(testTask)

        verify(exactly = 1) { taskRepositoryMock.save(capture(slot)) }
        assertEquals(AvstemMotOppdrag.TASK_STEP_TYPE, slot.captured.taskStepType)
        assertEquals(iDag.plusDays(1).toLocalDate().atTime(8, 0), slot.captured.triggerTid)
        val taskDTO = objectMapper.readValue(slot.captured.payload, AvstemmingTaskDTO::class.java)
        assertEquals(taskDTO.fomDato, iDag)
        assertEquals(taskDTO.tomDato, iDag.plusDays(1))
    }
}
