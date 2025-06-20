package no.nav.familie.ba.sak.kjerne.minside

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture

class MinsideAktiveringKafkaProducerTest {
    private val kafkaTemplate: KafkaTemplate<String, String> = mockk()
    private val minsideAktiveringKafkaProducer = MinsideAktiveringKafkaProducer(kafkaTemplate)

    @Nested
    inner class Aktiver {
        @Test
        fun `skal sende aktiveringsmelding for minside via Kafka`() {
            // Arrange

            val personIdent = "12345678901"

            val completableFuture: CompletableFuture<SendResult<String, String>> = mockk()
            val sendResult: SendResult<String, String> = mockk()

            every { kafkaTemplate.send(any(), any()) } returns completableFuture
            every { completableFuture.get(any(), any()) } returns sendResult

            // Act
            minsideAktiveringKafkaProducer.aktiver(personIdent)

            // Assert
            verify {
                kafkaTemplate.send(
                    "min-side.aapen-microfrontend-v1",
                    withArg {
                        assertTrue(it.contains("\"@action\":\"enable\""))
                        assertTrue(it.contains("\"ident\":\"12345678901\""))
                        assertTrue(it.contains("\"microfrontend_id\":\"familie-ba-mikrofrontend-minside\""))
                        assertTrue(it.contains("\"@initiated_by\":\"teamfamilie\""))
                        assertTrue(it.contains("\"sensitivitet\":\"high\""))
                    },
                )
            }
        }
    }

    @Nested
    inner class Deaktiver {
        @Test
        fun `skal sende deaktiveringsmelding for minside via Kafka`() {
            // Arrange
            val personIdent = "12345678901"

            val completableFuture: CompletableFuture<SendResult<String, String>> = mockk()
            val sendResult: SendResult<String, String> = mockk()

            every { kafkaTemplate.send(any(), any()) } returns completableFuture
            every { completableFuture.get(any(), any()) } returns sendResult

            // Act
            minsideAktiveringKafkaProducer.deaktiver(personIdent)

            // Assert
            verify {
                kafkaTemplate.send(
                    "min-side.aapen-microfrontend-v1",
                    withArg {
                        assertTrue(it.contains("\"@action\":\"disable\""))
                        assertTrue(it.contains("\"ident\":\"12345678901\""))
                        assertTrue(it.contains("\"microfrontend_id\":\"familie-ba-mikrofrontend-minside\""))
                        assertTrue(it.contains("\"@initiated_by\":\"teamfamilie\""))
                    },
                )
            }
        }
    }
}
