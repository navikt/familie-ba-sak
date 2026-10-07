package no.nav.familie.ba.sak.config

import io.mockk.MockKAnnotations
import io.mockk.mockk
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.kafka.listener.MessageListenerContainer

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaAivenErrorHandlerTest {
    private val container = mockk<MessageListenerContainer>(relaxed = true)
    private val consumer = mockk<Consumer<String, String>>(relaxed = true)

    private val errorHandler = KafkaAivenErrorHandler()

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `handle skal stoppe container hvis man mottar feil med en tom liste med records`() {
        // Act & Assert
        assertThatThrownBy {
            errorHandler.handleRemaining(
                RuntimeException("Feil i test"),
                emptyList(),
                consumer,
                container,
            )
        }.hasMessageNotContaining("Feil i test")
            .hasMessageContaining("Stopped container")
            .hasStackTraceContaining("Sjekk securelogs for mer info")
            .hasCauseExactlyInstanceOf(Exception::class.java)
    }

    @Test
    fun `handle skal stoppe container hvis man mottar feil med en liste med records`() {
        // Arrange
        val consumerRecord = ConsumerRecord("topic", 1, 1, 1, "record")

        // Act & Assert
        assertThatThrownBy {
            errorHandler.handleRemaining(
                RuntimeException("Feil i test"),
                listOf(consumerRecord),
                consumer,
                container,
            )
        }.hasMessageNotContaining("Feil i test")
            .hasMessageContaining("Stopped container")
            .hasStackTraceContaining("Sjekk securelogs for mer info")
            .hasCauseExactlyInstanceOf(Exception::class.java)
    }

    @Test
    fun `handleBatch skal stoppe container hvis batch-listener feiler`() {
        // Arrange
        val tp = TopicPartition("topic", 1)
        val records = ConsumerRecords(mapOf(tp to listOf(ConsumerRecord("topic", 1, 1, 1, "record"))), emptyMap())

        // Act & Assert
        assertThatThrownBy {
            errorHandler.handleBatch(RuntimeException("Feil i test"), records, consumer, container) {}
        }.hasMessageNotContaining("Feil i test")
            .hasMessageContaining("Stopped container")
            .hasStackTraceContaining("Sjekk securelogs for mer info")
    }

    @Test
    fun `handle skal stoppe container hvis man mottar feil hvor liste med records er empty`() {
        // Act & Assert
        assertThatThrownBy {
            errorHandler.handleRemaining(
                RuntimeException("Feil i test"),
                emptyList(),
                consumer,
                container,
            )
        }.hasMessageNotContaining("Feil i test")
            .hasMessageContaining("Stopped container")
            .hasStackTraceContaining("Sjekk securelogs for mer info")
            .hasCauseExactlyInstanceOf(Exception::class.java)
    }
}
