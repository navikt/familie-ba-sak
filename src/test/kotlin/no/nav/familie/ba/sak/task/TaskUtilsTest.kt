package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.core.env.Environment
import java.time.LocalDateTime

class TaskUtilsTest {

    @ParameterizedTest
    @CsvSource(
            "2020-06-09T13:37:00, 2020-06-10T13:37", //Neste dag er en arbeidsdag
            "2020-06-12T13:37:00, 2020-06-15T13:37", //Neste dag er en lørdag. Venter til mandag samme tid
            "2020-06-13T13:37:00, 2020-06-15T13:37", //Neste dag er en søndag. Venter til mandag samme tid
            "2020-06-13T06:37:00, 2020-06-15T10:37", //Ikke kjøre før kl 10 mandag
            "2020-06-11T16:01:00, 2020-06-15T10:01", //Ikke kjøre fredag etter 16. Vent til mandag
            "2021-05-14T00:00:00, 2021-05-18T10:00" //14 mai er fredag, 17 mai er mandag og fridag, skal returnere 18 mai
    )
    fun `skal returnere neste arbeidsdag `(input: LocalDateTime, expected: LocalDateTime) {
        mockkStatic(LocalDateTime::class)
        mockk<Environment>(relaxed = true)


        every { LocalDateTime.now() } returns input

        assertEquals(expected, nesteGyldigeTriggertidForBehandlingIHverdager(15, mockk(relaxed = true)))
    }
}