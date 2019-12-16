package no.nav.familie.ba.sak.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets

@TestConfiguration
@Profile("dev")
class RestTemplateConfig {

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate(listOf(StringHttpMessageConverter(StandardCharsets.UTF_8)))
    }
}
