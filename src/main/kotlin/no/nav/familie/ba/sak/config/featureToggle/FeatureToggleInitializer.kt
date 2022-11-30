package no.nav.familie.ba.sak.config.featureToggle

import no.nav.familie.ba.sak.config.FeatureToggleService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FeatureToggleInitializer(val featureToggleProperties: FeatureToggleProperties) {

    @Bean
    fun featureToggle(): FeatureToggleService =
        if (featureToggleProperties.enabled) {
            UnleashFeatureToggleService(featureToggleProperties.unleash)
        } else {
            logger.warn(
                "Funksjonsbryter-funksjonalitet er skrudd AV. " +
                    "Gir standardoppførsel for alle funksjonsbrytere, dvs 'false'"
            )
            DummyFeatureToggleService(featureToggleProperties.unleash)
        }

    companion object {

        private val logger = LoggerFactory.getLogger(FeatureToggleProperties::class.java)
    }
}
