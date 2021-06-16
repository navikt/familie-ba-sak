package no.nav.familie.ba.sak.common

import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse


class BearerAuthorizationInterceptor(private val oAuth2AccessTokenService: OAuth2AccessTokenService,
                                     private val clientProperties: ClientProperties) : ClientHttpRequestInterceptor {

    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)

        request.headers.setBearerAuth(response.accessToken)
        return execution.execute(request, body)
    }
}

