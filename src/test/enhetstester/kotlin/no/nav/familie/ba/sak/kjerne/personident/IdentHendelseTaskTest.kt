package no.nav.familie.ba.sak.kjerne.personident

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class IdentHendelseTaskTest {
    @MockK(relaxed = true)
    private lateinit var håndterNyIdentService: HåndterNyIdentService

    @InjectMockKs
    private lateinit var identHendelseTask: IdentHendelseTask

    @Test
    fun opprettTask() {
        val nyPersonIdent = PersonIdent("123")
        val task = IdentHendelseTask.opprettTask(nyPersonIdent)
        assertEquals(nyPersonIdent, objectMapper.readValue(task.payload, PersonIdent::class.java))
        assertEquals("123", task.metadata["nyPersonIdent"])
        assertEquals("IdentHendelseTask", task.type)

        identHendelseTask.doTask(task)

        val slot = slot<PersonIdent>()
        verify(exactly = 1) { håndterNyIdentService.håndterNyIdent(capture(slot)) }
        assertEquals(nyPersonIdent, slot.captured)
    }
}
