package no.nav.familie.ba.sak.config

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.familie.kontrakter.felles.objectMapper
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer


@Configuration
@Profile("!e2e")
class KafkaProducerConfig {


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
}