package no.nav.familie.ba.sak.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.familie.http.interceptor.BearerTokenWithSTSFallbackClientInterceptor
import no.nav.familie.http.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.http.interceptor.MdcValuesPropagatingClientInterceptor
import no.nav.familie.http.interceptor.StsBearerTokenClientInterceptor
import no.nav.familie.http.sts.StsRestClient
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Duration

@Configuration
@Import(
        ConsumerIdClientInterceptor::class,
        MdcValuesPropagatingClientInterceptor::class,
        StsBearerTokenClientInterceptor::class,
        BearerTokenWithSTSFallbackClientInterceptor::class)
@Profile("integrasjonstest")
class RestTemplateConfig {

    @Bean
    @Autowired
    @Profile("!mock-sts")
    fun stsRestClient(objectMapper: ObjectMapper,
                      @Value("\${STS_URL}") stsUrl: URI,
                      @Value("\${CREDENTIAL_USERNAME}") stsUsername: String,
                      @Value("\${CREDENTIAL_PASSWORD}") stsPassword: String): StsRestClient? {
        val stsFullUrl = URI.create("$stsUrl?grant_type=client_credentials&scope=openid")
        return StsRestClient(objectMapper, stsFullUrl, stsUsername, stsPassword)
    }

    @Bean("sts")
    fun restTemplateSts(stsBearerTokenClientInterceptor: StsBearerTokenClientInterceptor,
                        consumerIdClientInterceptor: ConsumerIdClientInterceptor): RestOperations {

        return RestTemplateBuilder()
                .interceptors(consumerIdClientInterceptor,
                              stsBearerTokenClientInterceptor,
                              MdcValuesPropagatingClientInterceptor())
                .requestFactory(this::requestFactory)
                .build()
    }
    private fun requestFactory() = HttpComponentsClientHttpRequestFactory()
            .apply {
                setConnectionRequestTimeout(20 * 1000)
                setReadTimeout(20 * 1000)
                setConnectTimeout(20 * 1000)
            }

    @Bean("jwt-sts")
    fun restTemplateJwtBearerFallbackSts(bearerTokenClientInterceptor: BearerTokenWithSTSFallbackClientInterceptor,
                                         consumerIdClientInterceptor: ConsumerIdClientInterceptor): RestOperations {

        return RestTemplateBuilder()
                .interceptors(consumerIdClientInterceptor,
                              bearerTokenClientInterceptor,
                              MdcValuesPropagatingClientInterceptor())
                .requestFactory(this::requestFactory)
                .build()
    }

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate(listOf(StringHttpMessageConverter(StandardCharsets.UTF_8),
            ByteArrayHttpMessageConverter(),
            MappingJackson2HttpMessageConverter(objectMapper)))
    }

    @Bean
    fun restTemplateBuilderMedProxy(consumerIdClientInterceptor: ConsumerIdClientInterceptor,
                                    mdcValuesPropagatingClientInterceptor: MdcValuesPropagatingClientInterceptor)
            : RestTemplateBuilder {
        return RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(5))
                .additionalInterceptors(consumerIdClientInterceptor, mdcValuesPropagatingClientInterceptor)
                .setReadTimeout(Duration.ofSeconds(5))
    }


    @Bean("jwtBearer")
    fun restTemplateClientCredentials(consumerIdClientInterceptor: ConsumerIdClientInterceptor,
                                      mdcValuesPropagatingClientInterceptor: MdcValuesPropagatingClientInterceptor)
            : RestOperations {
        return RestTemplateBuilder()
                .additionalInterceptors(consumerIdClientInterceptor, mdcValuesPropagatingClientInterceptor)
                .additionalMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
                .build()
    }
}