package no.nav.familie.ba.sak.arbeidsfordeling

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsfordelingsenhet
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

        val mockArbeidsfordelingService: ArbeidsfordelingService = mockk(relaxed = true)

        every { mockArbeidsfordelingService.hentArbeidsfordelingsenhet(any()) } returns Arbeidsfordelingsenhet(
            "100",
            "Mock enhet"
        )

        val slotBehandling = slot<Long>()
        every { mockArbeidsfordelingService.hentAbeidsfordelingPåBehandling(capture(slotBehandling)) } answers {
            ArbeidsfordelingPåBehandling(
                behandlingId = slotBehandling.captured,
                behandlendeEnhetId = "100", behandlendeEnhetNavn = "Mock Enhet"
            )
        }


        return mockArbeidsfordelingService
    }
}