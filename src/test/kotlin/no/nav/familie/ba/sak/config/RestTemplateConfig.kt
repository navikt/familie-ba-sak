package no.nav.familie.ba.sak.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
@Profile("dev")
class RestTemplateConfig {
    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplateBuilder()
                .build()
    }

    @Bean
    fun restTemplateBuilderMedProxy(): RestTemplateBuilder {
        return RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
    }
}