package no.nav.familie.ba.sak.kjerne.personident

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.Acknowledgment
import java.time.Duration

class IdenthendelseV2ConsumerTest {
    private val personidentService = mockk<PersonidentService>()
    private val ack = mockk<Acknowledgment>(relaxed = true)
    private val consumer = IdenthendelseV2Consumer(personidentService, Duration.ZERO)

    private val records = (0..2).map { lagRecord(offset = it.toLong(), aktørId = "100000000000$it", fnr = "1234567890$it") }

    @Test
    fun `skal kvittere hele batchen når alle meldinger lykkes`() {
        // Arrange
        every { personidentService.opprettTaskForIdentHendelse(any()) } just runs

        // Act
        consumer.listen(records, ack)

        // Assert
        verify(exactly = 3) { personidentService.opprettTaskForIdentHendelse(any()) }
        verify(exactly = 1) { ack.acknowledge() }
        verify(exactly = 0) { ack.acknowledge(any<Int>()) }
    }

    @Test
    fun `skal ikke kvittere noe når første melding feiler`() {
        // Arrange
        every { personidentService.opprettTaskForIdentHendelse(PersonIdent("12345678900")) } throws RuntimeException("feil")

        // Act & Assert
        assertThatThrownBy { consumer.listen(records, ack) }.isInstanceOf(RuntimeException::class.java)
        verify(exactly = 1) { personidentService.opprettTaskForIdentHendelse(any()) }
        verify(exactly = 0) { ack.acknowledge() }
        verify(exactly = 0) { ack.acknowledge(any<Int>()) }
    }

    @Test
    fun `skal kvittere meldingene før den som feiler når en senere melding feiler`() {
        // Arrange
        every { personidentService.opprettTaskForIdentHendelse(any()) } just runs
        every { personidentService.opprettTaskForIdentHendelse(PersonIdent("12345678902")) } throws RuntimeException("feil")

        // Act & Assert
        assertThatThrownBy { consumer.listen(records, ack) }.isInstanceOf(RuntimeException::class.java)
        verify(exactly = 3) { personidentService.opprettTaskForIdentHendelse(any()) }
        verify(exactly = 1) { ack.acknowledge(1) }
        verify(exactly = 0) { ack.acknowledge() }
    }

    private fun lagRecord(
        offset: Long,
        aktørId: String,
        fnr: String,
    ): ConsumerRecord<String, Aktor?> =
        ConsumerRecord(
            "pdl.aktor-v2",
            0,
            offset,
            aktørId,
            Aktor(
                listOf(
                    Identifikator(aktørId, Type.AKTORID, true),
                    Identifikator(fnr, Type.FOLKEREGISTERIDENT, true),
                ),
            ),
        )
}
