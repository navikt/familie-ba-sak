package no.nav.familie.ba.sak.cucumber.mock.komponentMocks

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.unleash.UnleashService

fun mockUnleashService(): UnleashService {
    val unleashService = mockk<UnleashService>()
    every { unleashService.isEnabled(any()) } returns true
    return unleashService
}
