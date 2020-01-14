package no.nav.familie.ba.sak.task

import io.mockk.mockk
import junit.framework.Assert.assertEquals
import no.nav.familie.ba.sak.økonomi.AvstemmingService
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
        val førsteJuledag = LocalDate.of(2019, 12,25)

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
        val førsteMai = LocalDate.of(2020, 5, 1)

        val testDto = avstemMotOppdrag.nesteAvstemmingDTO(førsteMai, 1)

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
}