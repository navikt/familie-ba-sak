package no.nav.familie.ba.sak.ekstern.pensjon.kafka

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.jsonMapper
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

class PensjonKafkaProducerTest {
    private val kafkaTemplate = mockk<KafkaTemplate<String, String>>()
    private val pensjonKafkaProducer = DefaultPensjonKafkaProducer(kafkaTemplate)

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

            val sendFuture = CompletableFuture.completedFuture(mockk<SendResult<String, String>>())

            every { kafkaTemplate.send(any(), any(), any()) } returns sendFuture

            // Act
            pensjonKafkaProducer.sendMeldingOmFerdigstiltBehandlingTilPensjon(hendelse)

            // Assert
            verify(exactly = 1) {
                kafkaTemplate.send(
                    DefaultPensjonKafkaProducer.AAPEN_IDENETER_MED_FERDIGSTILT_BEHANDLING_V1,
                    hendelse.ident,
                    jsonMapper.writeValueAsString(hendelse),
                )
            }
        }

        @Test
        fun `skal kaste feil dersom sending til Kafka feiler`() {
            // Arrange
            val hendelse =
                FerdigstilBehandlingHendelse(
                    ident = "12345678901",
                    vedtaktidspunkt = LocalDateTime.of(2026, 10, 8, 12, 0),
                    endringstidspunkt = LocalDate.of(2026, 10, 8),
                )

            every { kafkaTemplate.send(any(), any(), any()) } returns
                CompletableFuture.failedFuture(RuntimeException("Kafka nede"))

            // Act & Assert
            assertThatThrownBy { pensjonKafkaProducer.sendMeldingOmFerdigstiltBehandlingTilPensjon(hendelse) }
                .isInstanceOf(ExecutionException::class.java)
                .hasRootCauseMessage("Kafka nede")
        }
    }
}
