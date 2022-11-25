package no.nav.familie.ba.sak.config.featureToggle

import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService

class DummyFeatureToggleService(
    private val unleash: FeatureToggleConfig.Unleash
) : FeatureToggleService {
    override fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean {
        if (unleash.cluster == "lokalutvikling") {
            return false
        }

        return defaultValue
    }
}
