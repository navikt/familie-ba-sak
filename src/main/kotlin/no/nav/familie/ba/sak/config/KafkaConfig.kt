package no.nav.familie.ba.sak.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.familie.kontrakter.felles.objectMapper
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerConfigUtils
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.converter.StringJsonMessageConverter
import org.springframework.kafka.support.serializer.JsonSerializer
import java.time.Duration


@Configuration
@Profile("!e2e")
class KafkaConfig {

    @Bean
    fun kafkaObjectMapper() : ObjectMapper {
        return objectMapper.copy().setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }


    @Bean
    fun kafkaProducerFactory(properties: KafkaProperties): ProducerFactory<String, Any> {
        val mapper = objectMapper.copy().setSerializationInclusion(JsonInclude.Include.NON_NULL)
        val jsonSerializer: JsonSerializer<Any> = JsonSerializer(mapper)
        jsonSerializer.configure(properties.buildProducerProperties(), false)
        return DefaultKafkaProducerFactory(properties.buildProducerProperties(), StringSerializer(), jsonSerializer)
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, Any>): KafkaTemplate<String, Any> {
        return KafkaTemplate(producerFactory)
    }


    @Bean
    fun listenerContainerFactory(properties: KafkaProperties)
            : ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.containerProperties.authorizationExceptionRetryInterval = Duration.ofSeconds(2)
        factory.consumerFactory = DefaultKafkaConsumerFactory(properties.buildConsumerProperties())
        factory.setMessageConverter(StringJsonMessageConverter())
//        factory.setErrorHandler(kafkaErrorHandler)
        return factory
    }


    @Bean(name = [KafkaListenerConfigUtils.KAFKA_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME])
    fun kafkaListenerEndpointRegistry(): KafkaListenerEndpointRegistry? {
        return KafkaListenerEndpointRegistry()
    }
}