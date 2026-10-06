package no.nav.familie.ba.sak.ekstern.pensjon.kafka

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.jsonMapper
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

class PensjonKafkaProducerTest {
    private val kafkaTemplate = mockk<KafkaTemplate<String, String>>()
    private val pensjonKafkaProducer = PensjonKafkaProducer(kafkaTemplate)

    @Nested
    inner class SendMeldingOmFerdigstiltBehandlingTilPensjon {
        @Test
        fun `skal sende ferdigstilthendelse til pensjon via Kafka`() {
            // Arrange
            val hendelse =
                FerdigstilBehandlingHendelse(
                    ident = "12345678901",
                    vedtaktidspunkt = LocalDateTime.of(2026, 10, 8, 12, 0),
                    endringstidspunkt = LocalDate.of(2026, 10, 8),
                )

            val sendFuture: CompletableFuture<SendResult<String, String>> = mockk()

            every { kafkaTemplate.send(any(), any(), any()) } returns sendFuture

            // Act
            pensjonKafkaProducer.sendMeldingOmFerdigstiltBehandlingTilPensjon(hendelse)

            // Assert
            verify(exactly = 1) {
                kafkaTemplate.send(
                    PensjonKafkaProducer.AAPEN_IDENETER_MED_FERDIGSTILT_BEHANDLING_V1,
                    hendelse.ident,
                    jsonMapper.writeValueAsString(hendelse),
                )
            }
        }
    }
}
