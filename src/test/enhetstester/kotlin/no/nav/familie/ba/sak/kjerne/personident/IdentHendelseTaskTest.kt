package no.nav.familie.ba.sak.kjerne.personident

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.jsonMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class IdentHendelseTaskTest {
    private val håndterNyIdentService = mockk<HåndterNyIdentService>(relaxed = true)
    private val identHendelseTask = IdentHendelseTask(håndterNyIdentService)

    @Test
    fun opprettTask() {
        // Arrange
        val nyPersonIdent = PersonIdent("123")

        // Act
        val task = IdentHendelseTask.opprettTask(nyPersonIdent)

        // Assert
        assertEquals(nyPersonIdent, jsonMapper.readValue(task.payload, PersonIdent::class.java))
        assertEquals("123", task.metadata["nyPersonIdent"])
        assertEquals("IdentHendelseTask", task.type)

        // Act
        identHendelseTask.doTask(task)

        // Assert
        val slot = slot<PersonIdent>()
        verify(exactly = 1) { håndterNyIdentService.håndterNyIdent(capture(slot)) }
        assertEquals(nyPersonIdent, slot.captured)
    }
}
