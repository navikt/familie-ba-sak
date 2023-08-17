package no.nav.familie.ba.sak.config.featureToggle

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.getunleash.UnleashContextProvider
import io.getunleash.util.UnleashConfig
import no.nav.familie.ba.sak.config.FeatureToggleService

class UnleashNextFeatureToggleService(
    val apiUrl: String,
    val apiToken: String,
    val appName: String,
) : FeatureToggleService {

    private val defaultUnleash: DefaultUnleash

    init {

        defaultUnleash = DefaultUnleash(
            UnleashConfig.builder()
                .appName(appName)
                .unleashAPI(apiUrl)
                .apiKey(apiToken)
                .unleashContextProvider(lagUnleashContextProvider()).build(),
        )
    }

    private fun lagUnleashContextProvider(): UnleashContextProvider {
        return UnleashContextProvider {
            UnleashContext.builder()
                .appName(appName)
                .build()
        }
    }

    override fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean {
        return defaultUnleash.isEnabled(toggleId, defaultValue)
    }
}
