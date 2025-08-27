package no.nav.familie.ba.sak.cucumber.mock.komponentMocks

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.unleash.UnleashService

fun mockFeatureToggleService(): FeatureToggleService {
    val featureToggleService = mockk<FeatureToggleService>()
    every { featureToggleService.isEnabled(any<FeatureToggle>()) } returns true
    every { featureToggleService.isEnabled(any<FeatureToggle>(), any<Long>()) } returns true
    every { featureToggleService.isEnabled(any<FeatureToggle>(), any<Boolean>()) } returns true
    return featureToggleService
}

fun mockUnleashService(): UnleashService {
    val unleashService = mockk<UnleashService>()
    every { unleashService.isEnabled(any()) } returns true
    every { unleashService.isEnabled(any(), defaultValue = any()) } returns true
    return unleashService
}
