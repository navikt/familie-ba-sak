package no.nav.familie.ba.sak.config

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.kafka.listener.ContainerStoppingErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Component
class KafkaErrorHandler : ContainerStoppingErrorHandler() {

    val LOGGER: Logger = LoggerFactory.getLogger(KafkaErrorHandler::class.java)
    val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLogger")

    private val executor: Executor
    private val counter = AtomicInteger(0)
    private val lastError = AtomicLong(0)
    override fun handle(e: Exception,
                        records: List<ConsumerRecord<*, *>>?,
                        consumer: Consumer<*, *>,
                        container: MessageListenerContainer) {
        Thread.sleep(1000)

        if (records.isNullOrEmpty()) {
            LOGGER.error("Feil ved konsumering av melding. Ingen records. ${consumer.subscription()}", e)
            scheduleRestart(e,
                    records,
                    consumer,
                    container,
                    "Ukjent topic")
        } else {
            records.first().run {
                LOGGER.error("Feil ved konsumering av melding fra ${this.topic()}. id ${this.key()}, " +
                        "offset: ${this.offset()}, partition: ${this.partition()}")
                SECURE_LOGGER.error("${this.topic()} - Problemer med prosessering av $records", e)
                scheduleRestart(e,
                        records,
                        consumer,
                        container,
                        this.topic())
            }
        }
    }

    private fun scheduleRestart(e: Exception,
                                records: List<ConsumerRecord<*, *>>? = null,
                                consumer: Consumer<*, *>,
                                container: MessageListenerContainer,
                                topic: String) {
        val now = System.currentTimeMillis()
        if (now - lastError.getAndSet(now) > COUNTER_RESET_TIME) {
            counter.set(0)
        }
        val numErrors = counter.incrementAndGet()
        val stopTime =
                if (numErrors > SLOW_ERROR_COUNT) LONG else SHORT * numErrors
        executor.execute {
            try {
                Thread.sleep(stopTime)
                LOGGER.warn("Starter kafka container for {}", topic)
                container.start()
            } catch (exception: Exception) {
                LOGGER.error("Feil oppstod ved venting og oppstart av kafka container", exception)
            }
        }
        LOGGER.warn("Stopper kafka container for {} i {}", topic, Duration.ofMillis(stopTime).toString())
        super.handle(e, records, consumer, container)
    }

    companion object {
        private val LONG = Duration.ofHours(3).toMillis()
        private val SHORT = Duration.ofSeconds(20).toMillis()
        private const val SLOW_ERROR_COUNT = 10
        private val COUNTER_RESET_TIME =
                SHORT * SLOW_ERROR_COUNT * 2
    }

    init {
        this.executor = SimpleAsyncTaskExecutor()
    }
}