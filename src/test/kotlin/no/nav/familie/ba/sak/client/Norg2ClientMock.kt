package no.nav.familie.ba.sak.client

import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class Norg2ClientMock {

    @Bean
    @Primary
    fun mockNorg2RestClient(): Norg2RestClient {
        val norg2RestClient = mockk<Norg2RestClient>()
        every { norg2RestClient.hentEnhet(any()) } returns Enhet(enhetId = 1234L, navn = "Mock Enhet")

        return norg2RestClient
    }
}