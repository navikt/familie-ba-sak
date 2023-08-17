package no.nav.familie.ba.sak.config.featureToggle

import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.config.featureToggle.miljø.Profil
import no.nav.familie.ba.sak.config.featureToggle.miljø.erAktiv
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class FeatureToggleInitializer(
    private val featureToggleProperties: FeatureToggleProperties,
    private val environment: Environment,
    @Value("\${UNLEASH_SERVER_API_URL}") val apiUrl: String,
    @Value("\${UNLEASH_SERVER_API_TOKEN}") val apiToken: String,
    @Value("\${NAIS_APP_NAME}") val appName: String,
) {

    @Bean
    fun featureToggle(): FeatureToggleService =
        if (featureToggleProperties.enabled || environment.erAktiv(Profil.DevPostgresPreprod)) {
            UnleashFeatureToggleService(featureToggleProperties.unleash)
        } else {
            logger.warn(
                "Funksjonsbryter-funksjonalitet er skrudd AV. " +
                    "Gir standardoppførsel for alle funksjonsbrytere, dvs 'false'",
            )
            DummyFeatureToggleService(featureToggleProperties.unleash)
        }

    @Bean("unleashNext")
    fun unleashNext(): FeatureToggleService =
        if (featureToggleProperties.enabled || environment.erAktiv(Profil.DevPostgresPreprod)) {
            UnleashNextFeatureToggleService(apiUrl = apiUrl, apiToken = apiToken, appName = appName)
        } else {
            logger.warn(
                "Funksjonsbryter-funksjonalitet er skrudd AV. " +
                    "Gir standardoppførsel for alle funksjonsbrytere, dvs 'false'",
            )
            DummyFeatureToggleService(featureToggleProperties.unleash)
        }

    companion object {

        private val logger = LoggerFactory.getLogger(FeatureToggleProperties::class.java)
    }
}
