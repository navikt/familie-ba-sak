package no.nav.familie.ba.sak.config

import no.nav.familie.unleash.UnleashService
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class MockUnleashService: UnleashService {

    override fun isEnabled(toggleId: String): Boolean {
        val mockUnleashServiceAnswer = System.getProperty("mockFeatureToggleAnswer")?.toBoolean() ?: true
        return System.getProperty(toggleId)?.toBoolean() ?: mockUnleashServiceAnswer
    }

    override fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean {
        return isEnabled(toggleId)
    }

    override fun isEnabled(toggleId: String, properties: Map<String, String>): Boolean {
        return isEnabled(toggleId)
    }

    override fun destroy() {}
}
