package no.nav.familie.ba.sak.config

import no.finn.unleash.*
import no.finn.unleash.strategy.Strategy
import no.finn.unleash.util.UnleashConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import java.net.URI

@ConfigurationProperties("funksjonsbrytere")
@ConstructorBinding
class FeatureToggleConfig(private val enabled: Boolean,
                          val unleash: Unleash) {

    @ConstructorBinding
    data class Unleash(val uri: URI,
                       val environment: String,
                       val applicationName: String)

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    @Bean
    fun featureToggle(): FeatureToggleService =
            if (enabled)
                lagUnleashFeatureToggleService()
            else {
                log.warn("Funksjonsbryter-funksjonalitet er skrudd AV. " +
                         "Gir standardoppførsel for alle funksjonsbrytere, dvs 'false'")
                lagDummyFeatureToggleService()
            }

    private fun lagUnleashFeatureToggleService(): FeatureToggleService {
        val unleash = DefaultUnleash(UnleashConfig.builder()
                                             .appName(unleash.applicationName)
                                             .unleashAPI(unleash.uri)
                                             .unleashContextProvider(lagUnleashContextProvider())
                                             .build(), ByEnvironmentStrategy())

        return object : FeatureToggleService {
            override fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean {
                return unleash.isEnabled(toggleId, defaultValue)
            }
        }
    }

    private fun lagUnleashContextProvider(): UnleashContextProvider {
        return UnleashContextProvider {
            UnleashContext.builder()
                    .environment(unleash.environment)
                    .appName(unleash.applicationName)
                    .build()
        }
    }

    private fun lagDummyFeatureToggleService(): FeatureToggleService {
        return object : FeatureToggleService {
            override fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean {
                if (unleash.environment == "dev") {
                    return true
                }
                return defaultValue
            }
        }
    }

}

class ByEnvironmentStrategy : Strategy {

    companion object {
        private const val miljøKey = "miljø"

        fun lagPropertyMapMedMiljø(vararg strings: String): Map<String, String> {
            return mapOf(miljøKey to strings.joinToString(","))
        }
    }

    override fun getName(): String {
        return "byEnvironment"
    }

    override fun isEnabled(map: Map<String, String>?): Boolean {
        return isEnabled(map, UnleashContext.builder().build())
    }

    override fun isEnabled(map: Map<String, String>?, unleashContext: UnleashContext): Boolean {

        return unleashContext.environment
                .map { env -> map?.get(miljøKey)?.split(',')?.contains(env) ?: false }
                .orElse(false)
    }

}

interface FeatureToggleService {

    fun isEnabled(toggleId: String): Boolean {
        return isEnabled(toggleId, false)
    }

    fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean
}

