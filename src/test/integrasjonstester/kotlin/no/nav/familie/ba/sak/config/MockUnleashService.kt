package no.nav.familie.ba.sak.config

import no.nav.familie.unleash.UnleashService
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Primary
@Profile("mock-unleash")
class MockUnleashService : UnleashService {
    override fun isEnabled(toggleId: String): Boolean {
        val mockFeatureToggleAnswer = System.getProperty("mockFeatureToggleAnswer")?.toBoolean()
        if (mockFeatureToggleAnswer == null) throw Exception("mockFeatureToggleAnswer system property is not set")
        if (mockFeatureToggleAnswer == false) throw Exception("mockFeatureToggleAnswer system property is false")
        if (mockFeatureToggleAnswer == true) throw Exception("mockFeatureToggleAnswer system property is false")

        val mockUnleashServiceAnswer = System.getProperty("mockFeatureToggleAnswer")?.toBoolean() ?: true
        return System.getProperty(toggleId)?.toBoolean() ?: mockUnleashServiceAnswer
    }

    override fun isEnabled(
        toggleId: String,
        defaultValue: Boolean,
    ): Boolean = isEnabled(toggleId)

    override fun isEnabled(
        toggleId: String,
        properties: Map<String, String>,
    ): Boolean = isEnabled(toggleId)

    override fun destroy() {}
}
