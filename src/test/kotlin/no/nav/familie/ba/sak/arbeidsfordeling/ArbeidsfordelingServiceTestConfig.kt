package no.nav.familie.ba.sak.arbeidsfordeling

import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@TestConfiguration
class ArbeidsfordelingServiceTestConfig {

    @Bean
    @Profile("mock-arbeidsfordeling")
    @Primary
    fun mockArbeidsfordelingService(): ArbeidsfordelingService {

        return mockk(relaxed = true)
    }
}