package no.nav.familie.ba.sak.config

import no.nav.familie.kontrakter.felles.jsonMapperBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ObjectMapper

@Configuration
class JacksonJsonConfig {
    @Bean
    fun objectMapper(): ObjectMapper =
        jsonMapperBuilder
            .build()
}
