package no.nav.familie.ba.sak.cucumber.mock.komponentMocks

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.unleash.UnleashService

fun mockUnleashNextMedContextService(): UnleashNextMedContextService {
    val unleashNextMedContextService = mockk<UnleashNextMedContextService>()
    every { unleashNextMedContextService.isEnabled(any<FeatureToggle>()) } returns true
    every { unleashNextMedContextService.isEnabled(any<FeatureToggle>(), any<Long>()) } returns true
    every { unleashNextMedContextService.isEnabled(any<FeatureToggle>(), any<Boolean>()) } returns true
    return unleashNextMedContextService
}

fun mockUnleashService(): UnleashService {
    val unleashService = mockk<UnleashService>()
    every { unleashService.isEnabled(any()) } returns true
    every { unleashService.isEnabled(any(), defaultValue = any()) } returns true
    return unleashService
}
