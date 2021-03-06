package no.nav.familie.ba.sak.config

import no.finn.unleash.DefaultUnleash
import no.finn.unleash.UnleashContext
import no.finn.unleash.UnleashContextProvider
import no.finn.unleash.strategy.Strategy
import no.finn.unleash.util.UnleashConfig
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
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
                       val cluster: String,
                       val applicationName: String)

    @Bean
    fun featureToggle(): FeatureToggleService =
            if (enabled)
                lagUnleashFeatureToggleService()
            else {
                logger.warn("Funksjonsbryter-funksjonalitet er skrudd AV. " +
                            "Gir standardoppførsel for alle funksjonsbrytere, dvs 'false'")
                lagDummyFeatureToggleService()
            }

    private fun lagUnleashFeatureToggleService(): FeatureToggleService {
        val defaultUnleash = DefaultUnleash(UnleashConfig.builder()
                                                    .appName(unleash.applicationName)
                                                    .unleashAPI(unleash.uri)
                                                    .unleashContextProvider(lagUnleashContextProvider())
                                                    .build(),
                                            ByClusterStrategy(unleash.cluster),
                                            ByAnsvarligSaksbehandler())

        return object : FeatureToggleService {
            override fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean {
                return defaultUnleash.isEnabled(toggleId, defaultValue)
            }
        }

    }

    private fun lagUnleashContextProvider(): UnleashContextProvider {
        return UnleashContextProvider {
            UnleashContext.builder()
                    .appName(unleash.applicationName)
                    .build()
        }
    }

    class ByClusterStrategy(private val clusterName: String) : Strategy {

        override fun isEnabled(parameters: MutableMap<String, String>): Boolean {
            if (parameters.isEmpty()) return false
            return parameters["cluster"]?.contains(clusterName) ?: false
        }

        override fun getName(): String = "byCluster"
    }

    class ByAnsvarligSaksbehandler : Strategy {

        override fun isEnabled(parameters: MutableMap<String, String>): Boolean {
            if (parameters.isEmpty()) return false

            return parameters["saksbehandler"]?.contains(SikkerhetContext.hentSaksbehandler()) ?: false
        }

        override fun getName(): String = "byAnsvarligSaksbehandler"
    }

    private fun lagDummyFeatureToggleService(): FeatureToggleService {
        return object : FeatureToggleService {
            override fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean {
                if (unleash.cluster == "lokalutvikling") {
                    return false
                }

                return defaultValue
            }
        }
    }

    companion object {
        const val TILBAKEKREVING = "familie-ba-sak.behandling.tilbakekreving"
        const val BRUK_VEDTAKSTYPE_MED_BEGRUNNELSER = "familie-ba-sak.behandling.vedtakstype-med-begrunnelser"
        const val AUTOMATISK_FØDSELSHENDELSE = "familie-ba-sak.behandling.automatisk-fødselshendelse"
        const val BRUK_ER_DELT_BOSTED = "familie-ba-sak.behandling.delt_bosted"
        const val MIGRER_VEDTAK_BEGRUNNELSES_MODEL_UTREDNING = "familie-ba-sak.behandling.migrering.behandlingsmodel.utredning"
        const val KJØR_SATSENDRING_SCHEDULER = "familie-ba-sak.satsendring-scheduler"

        private val logger = LoggerFactory.getLogger(FeatureToggleConfig::class.java)
    }
}

interface FeatureToggleService {

    fun isEnabled(toggleId: String): Boolean {
        return isEnabled(toggleId, false)
    }

    fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean
}

