package no.nav.familie.ba.sak.client

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class Norg2ClientMock {

    @Bean
    @Primary
    fun mockNorg2RestClient(): Norg2RestClient {
        val norg2RestClient = mockk<Norg2RestClient>()

        val hentEnhetSlot = slot<String>()
        every { norg2RestClient.hentEnhet(capture(hentEnhetSlot)) } answers {
            Enhet(enhetId = hentEnhetSlot.captured.toLong(),
                  navn = "${hentEnhetSlot.captured}, NAV Familie- og pensjonsytelser Oslo 1")
        }

        return norg2RestClient
    }
}