package no.nav.familie.ba.sak.cucumber.mock.komponentMocks

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService

fun mockUnleashNextMedContextService(): UnleashNextMedContextService {
    val unleashNextMedContextService = mockk<UnleashNextMedContextService>()
    every { unleashNextMedContextService.isEnabled(any()) } returns true
    return unleashNextMedContextService
}
