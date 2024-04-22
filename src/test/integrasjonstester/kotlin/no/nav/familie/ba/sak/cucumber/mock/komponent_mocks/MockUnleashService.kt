package no.nav.familie.ba.sak.cucumber.mock.komponent_mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.TILKJENT_YTELSE_STONAD_TOM
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService

fun mockUnleashService(): UnleashNextMedContextService {
    val unleashService = mockk<UnleashNextMedContextService>()
    every { unleashService.isEnabled(TILKJENT_YTELSE_STONAD_TOM) } returns true
    return unleashService
}
