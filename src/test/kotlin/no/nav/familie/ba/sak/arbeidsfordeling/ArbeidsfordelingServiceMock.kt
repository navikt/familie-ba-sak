package no.nav.familie.ba.sak.arbeidsfordeling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsfordelingsenhet
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class ArbeidsfordelingServiceMock {

    @Bean
    @Primary
    fun mockArbeidsfordelingService(): ArbeidsfordelingService {
        val arbeidsfordelingServiceMock: ArbeidsfordelingService = mockk()
        every { arbeidsfordelingServiceMock.hentBehandlendeEnhet(any()) } returns listOf(Arbeidsfordelingsenhet(enhetId = "1234",
                                                                                                                enhetNavn = "Mock enhet"))
        every { arbeidsfordelingServiceMock.bestemBehandlendeEnhet(any()) } returns "1234"

        return arbeidsfordelingServiceMock
    }
}