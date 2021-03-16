package no.nav.familie.ba.sak.config

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.familie.ba.sak.saksstatistikk.SaksstatistikkEventPublisher
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@TestConfiguration
class SakstatistikkTestConfig {

    @Bean
    @Profile("mock-sakstatistikk")
    @Primary
    fun mockSaksstatistikkEventPublisher(): SaksstatistikkEventPublisher {
        val saksstatistikkEventPublisher: SaksstatistikkEventPublisher = mockk()
        every { saksstatistikkEventPublisher.publiserBehandlingsstatistikk(any(), any()) } just Runs
        every { saksstatistikkEventPublisher.publiserSaksstatistikk(any()) } just Runs
        return saksstatistikkEventPublisher
    }
}