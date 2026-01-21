package no.nav.familie.ba.sak.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ObjectMapper

@Configuration
class JacksonJsonConfig {
    @Bean
    fun jsonMapper(): ObjectMapper = jsonMapper()
}
