package no.nav.familie.ba.sak.integrasjoner

import no.nav.familie.ba.sak.common.BaseService
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.stereotype.Component
import java.net.URI

@Component
class IntegrasjonTjeneste(
        @Value("\${FAMILIE_INTEGRASJONER_API_URL}")
        private val integrasjonerServiceUri: URI,
        restTemplateBuilderMedProxy: RestTemplateBuilder,
        clientConfigurationProperties: ClientConfigurationProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService,
        private val featureToggleService: FeatureToggleService
) : BaseService(OAUTH2_CLIENT_CONFIG_KEY, restTemplateBuilderMedProxy, clientConfigurationProperties, oAuth2AccessTokenService) {



    companion object {
        private val logger = LoggerFactory.getLogger(IntegrasjonTjeneste::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")

        const val VEDTAK_DOKUMENT_TYPE = "BARNETRYGD_VEDTAK"

        private const val OAUTH2_CLIENT_CONFIG_KEY = "familie-integrasjoner-clientcredentials"
        private const val TEMA = "BAR"
    }
}
