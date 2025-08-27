package no.nav.familie.ba.sak.config

import io.mockk.mockk
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Primary
@Profile("mock-unleash")
class MockFeatureToggleService :
    FeatureToggleService(
        unleashService = mockk(),
        behandlingHentOgPersisterService = mockk(),
        arbeidsfordelingPåBehandlingRepository = mockk(),
    ) {
    override fun isEnabled(toggleId: String): Boolean {
        val mockUnleashServiceAnswer = System.getProperty("mockFeatureToggleAnswer")?.toBoolean() ?: true
        return System.getProperty(toggleId)?.toBoolean() ?: mockUnleashServiceAnswer
    }

    override fun isEnabled(
        toggle: FeatureToggle,
    ): Boolean = isEnabled(toggle.navn)

    override fun isEnabled(
        toggle: FeatureToggle,
        defaultValue: Boolean,
    ): Boolean = isEnabled(toggle.navn)

    override fun isEnabled(
        toggle: FeatureToggle,
        behandlingId: Long,
    ): Boolean = isEnabled(toggle.navn)
}
