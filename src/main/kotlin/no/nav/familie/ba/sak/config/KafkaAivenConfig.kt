package no.nav.familie.ba.sak.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import no.nav.familie.kontrakter.felles.Applikasjon
import no.nav.familie.kontrakter.felles.objectMapper
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerConfigUtils
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.LoggingProducerListener

@Configuration
class KafkaAivenConfig(
    val environment: Environment,
) {
    @Bean
    fun producerFactory(): ProducerFactory<String, String> = DefaultKafkaProducerFactory(producerConfigs())

    @Bean
    fun kafkaAivenTemplate(): KafkaTemplate<String, String> {
        val producerListener = LoggingProducerListener<String, String>()
        producerListener.setIncludeContents(false)
        return KafkaTemplate(producerFactory()).apply<KafkaTemplate<String, String>> {
            setProducerListener(producerListener)
        }
    }

    @Bean
    fun consumerFactory(): ConsumerFactory<String, String> = DefaultKafkaConsumerFactory(consumerConfigs())

    @Bean
    fun concurrentKafkaListenerContainerFactory(kafkaErrorHandler: KafkaAivenErrorHandler): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.setConcurrency(1)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        factory.setConsumerFactory(consumerFactory())
        factory.setCommonErrorHandler(kafkaErrorHandler)
        return factory
    }

    @Bean
    fun kafkaAivenHendelseListenerAvroLatestContainerFactory(kafkaErrorHandler: KafkaAivenErrorHandler): ConcurrentKafkaListenerContainerFactory<String, String> =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
            setConsumerFactory(DefaultKafkaConsumerFactory(consumerConfigsLatestAvro()))
            setCommonErrorHandler(kafkaErrorHandler)
        }

    @Bean(name = [KafkaListenerConfigUtils.KAFKA_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME])
    fun kafkaListenerEndpointRegistry(): KafkaListenerEndpointRegistry? = KafkaListenerEndpointRegistry()

    @Bean("kafkaObjectMapper")
    fun kafkaObjectMapper(): ObjectMapper = objectMapper.copy().setSerializationInclusion(JsonInclude.Include.NON_NULL)

    private fun producerConfigs(): Map<String, Any> {
        val kafkaBrokers = System.getenv("KAFKA_BROKERS") ?: LOCAL_KAFKA_BROKER
        val producerConfigs =
            mutableMapOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaBrokers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                // Den sikrer rekkefølge
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
                // Den sikrer at data ikke mistes
                ProducerConfig.ACKS_CONFIG to "all",
                ProducerConfig.CLIENT_ID_CONFIG to Applikasjon.FAMILIE_BA_SAK.name,
            )
        if (environment.activeProfiles.none { it.contains("dev") || it.contains("postgres") }) {
            return producerConfigs + securityConfig()
        }
        return producerConfigs.toMap()
    }

    fun consumerConfigs(): Map<String, Any> {
        val kafkaBrokers = System.getenv("KAFKA_BROKERS") ?: LOCAL_KAFKA_BROKER
        val consumerConfigs =
            mutableMapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaBrokers,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.GROUP_ID_CONFIG to "familie-ba-sak",
                ConsumerConfig.CLIENT_ID_CONFIG to "consumer-familie-ba-sak-1",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest",
            )
        if (environment.activeProfiles.none { it.contains("dev") || it.contains("postgres") }) {
            return consumerConfigs + securityConfig()
        }
        return consumerConfigs.toMap()
    }

    private fun consumerConfigsLatestAvro(): Map<String, Any> {
        val kafkaBrokers = System.getenv("KAFKA_BROKERS") ?: LOCAL_KAFKA_BROKER
        val schemaRegisty = System.getenv("KAFKA_SCHEMA_REGISTRY") ?: "http://localhost:9093"
        val schemaRegistryUser = System.getenv("KAFKA_SCHEMA_REGISTRY_USER") ?: "mangler i pod"
        val schemaRegistryPassword = System.getenv("KAFKA_SCHEMA_REGISTRY_PASSWORD") ?: "mangler i pod"
        val consumerConfigs =
            mutableMapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaBrokers,
                "schema.registry.url" to schemaRegisty,
                "basic.auth.credentials.source" to "USER_INFO",
                "basic.auth.user.info" to "$schemaRegistryUser:$schemaRegistryPassword",
                "specific.avro.reader" to true,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
                ConsumerConfig.CLIENT_ID_CONFIG to "consumer-familie-ba-sak-2",
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "false",
            )
        if (environment.activeProfiles.none { it.contains("dev") || it.contains("postgres") }) {
            return consumerConfigs + securityConfig()
        }
        return consumerConfigs.toMap()
    }

    private fun securityConfig(): Map<String, Any> {
        val kafkaTruststorePath = System.getenv("KAFKA_TRUSTSTORE_PATH")
        val kafkaCredstorePassword = System.getenv("KAFKA_CREDSTORE_PASSWORD")
        val kafkaKeystorePath = System.getenv("KAFKA_KEYSTORE_PATH")
        return mapOf(
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
            // Disable server host name verification
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "",
            SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to "JKS",
            SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to "PKCS12",
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to kafkaTruststorePath,
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to kafkaKeystorePath,
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
            SslConfigs.SSL_KEY_PASSWORD_CONFIG to kafkaCredstorePassword,
        )
    }

    companion object {
        const val LOCAL_KAFKA_BROKER = "http://localhost:9092"
    }
}
