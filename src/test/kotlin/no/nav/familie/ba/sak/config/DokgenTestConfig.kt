package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.behandling.DokGenService

import org.mockito.Mockito.*
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@TestConfiguration
class DokgenTestConfig {

    @Bean
    @Profile("mock-dokgen")
    @Primary
    fun mockDokGenService(): DokGenService {
        val dokgenService = mock(DokGenService::class.java)
        return dokgenService
    }
}