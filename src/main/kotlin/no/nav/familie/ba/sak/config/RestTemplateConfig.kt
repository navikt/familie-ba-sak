package no.nav.familie.ba.sak.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestTemplate

@Configuration
@Profile("!dev")
class RestTemplateConfig {
    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }

    @Bean
    fun restTemplateMedProxy(): RestTemplate {
        return RestTemplateBuilder()
            .additionalCustomizers(NaisProxyCustomizer())
            .build()
    }

    @Bean
    fun restTemplateBuilderMedProxy(): RestTemplateBuilder {
        return RestTemplateBuilder()
            .additionalCustomizers(NaisProxyCustomizer())
    }
}