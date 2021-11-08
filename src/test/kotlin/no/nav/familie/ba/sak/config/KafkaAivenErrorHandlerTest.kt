package no.nav.familie.ba.sak.config

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.kafka.listener.MessageListenerContainer

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaAivenErrorHandlerTest {

    @MockK(relaxed = true)
    lateinit var container: MessageListenerContainer

    @MockK(relaxed = true)
    lateinit var consumer: Consumer<*, *>

    @InjectMockKs
    lateinit var errorHandler: KafkaAivenErrorHandler

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `handle skal stoppe container hvis man mottar feil med en tom liste med records`() {
        assertThatThrownBy { errorHandler.handle(RuntimeException("Feil i test"), emptyList(), consumer, container) }
            .hasMessageContaining("Feil i test")
            .hasCauseExactlyInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `handle skal stoppe container hvis man mottar feil med en liste med records`() {
        val consumerRecord = ConsumerRecord("topic", 1, 1, 1, "record")
        assertThatThrownBy {
            errorHandler.handle(
                RuntimeException("Feil i test"),
                listOf(consumerRecord),
                consumer,
                container
            )
        }
            .hasMessageContaining("Feil i test")
            .hasCauseExactlyInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `handle skal stoppe container hvis man mottar feil hvor liste med records er null`() {
        assertThatThrownBy { errorHandler.handle(RuntimeException("Feil i test"), null, consumer, container) }
            .hasMessageContaining("Feil i test")
            .hasCauseExactlyInstanceOf(RuntimeException::class.java)
    }
}