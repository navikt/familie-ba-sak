package no.nav.familie.ba.sak.config

import io.mockk.mockk
import no.nav.familie.ba.sak.common.LocalDateService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@TestConfiguration
class LocalDateServiceTestConfig {
    @Bean
    @Profile("mock-localdate-service")
    @Primary
    fun mockLocalDateService(): LocalDateService {
        return mockk(relaxed = true)
    }
}