package no.nav.familie.ba.sak.common

import no.nav.familie.http.interceptor.MdcValuesPropagatingClientInterceptor
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.NavHttpHeaders
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.*
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestOperations
import org.springframework.web.client.exchange
import java.net.URI
import java.util.*


class BearerAuthorizationInterceptor(private val oAuth2AccessTokenService: OAuth2AccessTokenService,
                                     private val clientProperties: ClientProperties) : ClientHttpRequestInterceptor {

    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)

        request.headers.setBearerAuth(response.accessToken)
        return execution.execute(request, body)
    }
}

open class BaseService(clientConfigKey: String, restTemplateBuilder: RestTemplateBuilder,
                       clientConfigurationProperties: ClientConfigurationProperties,
                       oAuth2AccessTokenService: OAuth2AccessTokenService) {

    private val clientProperties: ClientProperties = Optional.ofNullable(
            clientConfigurationProperties.registration[clientConfigKey])
            .orElseThrow { RuntimeException("could not find oauth2 client config for key=$clientConfigKey") }
    val restOperations: RestOperations = restTemplateBuilder
            .additionalInterceptors(BearerAuthorizationInterceptor(oAuth2AccessTokenService, clientProperties),
                                    MdcValuesPropagatingClientInterceptor())
            .additionalMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
            .build()

    protected inline fun <reified T> requestMedPersonIdent(uri: URI, personident: String): ResponseEntity<T> {
        val headers: MultiValueMap<String, String> = LinkedMultiValueMap()
        headers.add(NavHttpHeaders.NAV_PERSONIDENT.asString(), personident)
        val httpEntity: HttpEntity<*> = HttpEntity<Any?>(headers)
        return restOperations.exchange(uri, HttpMethod.GET, httpEntity)
    }

    protected fun HttpHeaders.medContentTypeJsonUTF8(): HttpHeaders {
        this.add("Content-Type", "application/json;charset=UTF-8")
        this.acceptCharset = listOf(Charsets.UTF_8)
        return this
    }
}
