package no.nav.familie.ba.sak.common

import no.nav.familie.http.sts.StsRestClient
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.OAuth2GrantType
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import java.net.URI

@Component
class BearerTokenWithSTSFallbackClientInterceptor(private val oAuth2AccessTokenService: OAuth2AccessTokenService,
                                                  private val clientConfigurationProperties: ClientConfigurationProperties,
                                                  private val stsRestClient: StsRestClient) :
        ClientHttpRequestInterceptor {

    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        if (preferredUsername() == null) {
            request.headers.setBearerAuth(stsRestClient.systemOIDCToken)
        } else {
            val clientProperties = clientPropertiesFor(request.uri)
            val response: OAuth2AccessTokenResponse = oAuth2AccessTokenService.getAccessToken(clientProperties)
            request.headers.setBearerAuth(response.accessToken)
        }

        return execution.execute(request, body)
    }

    private fun clientPropertiesFor(uri: URI): ClientProperties {
        val values = clientConfigurationProperties
                .registration
                .values
                .filter { uri.toString().startsWith(it.resourceUrl.toString()) }
        return if (values.size == 1) values.first() else filterForGrantType(values, uri)
    }

    private fun filterForGrantType(values: List<ClientProperties>, uri: URI): ClientProperties {
        val preferredUsername = preferredUsername()
        val grantType = if (preferredUsername == null) OAuth2GrantType.CLIENT_CREDENTIALS else OAuth2GrantType.JWT_BEARER
        return values.firstOrNull { grantType == it.grantType }
                ?: error("could not find oauth2 client config for uri=$uri and grant type=$grantType")
    }

    private fun preferredUsername(): Any? {
        return try {
            SpringTokenValidationContextHolder().tokenValidationContext.getClaims("azuread")["preferred_username"]
        } catch (e: Throwable) {
            // Ingen request context. Skjer ved kall som har opphav i kjørende applikasjon. Ping etc.
            null
        }
    }
}
