package no.nav.familie.ba.sak.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.familie.ba.sak.common.BearerTokenWithSTSFallbackClientInterceptor
import no.nav.familie.http.interceptor.BearerTokenClientInterceptor
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
import org.springframework.core.env.Environment
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
        BearerTokenClientInterceptor::class,
        MdcValuesPropagatingClientInterceptor::class,
        StsBearerTokenClientInterceptor::class,
        BearerTokenWithSTSFallbackClientInterceptor::class)
class RestTemplateConfig(
        private val environment: Environment
) {

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

        return if (trengerProxy()) {
            RestTemplateBuilder()
                    .additionalCustomizers(NaisProxyCustomizer())
                    .interceptors(consumerIdClientInterceptor,
                                  bearerTokenClientInterceptor,
                                  MdcValuesPropagatingClientInterceptor())
                    .additionalMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
                    .build()
        } else {
            RestTemplateBuilder()
                    .interceptors(consumerIdClientInterceptor,
                                  bearerTokenClientInterceptor,
                                  MdcValuesPropagatingClientInterceptor())
                    .additionalMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
                    .build()
        }
    }

    @Bean("jwtBearer")
    fun restTemplateJwtBearer(consumerIdClientInterceptor: ConsumerIdClientInterceptor,
                              bearerTokenClientInterceptor: BearerTokenClientInterceptor): RestOperations {
        return if (trengerProxy()) {
            RestTemplateBuilder()
                    .additionalCustomizers(NaisProxyCustomizer())
                    .interceptors(consumerIdClientInterceptor,
                                  bearerTokenClientInterceptor,
                                  MdcValuesPropagatingClientInterceptor())
                    .additionalMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
                    .build()
        } else {
            RestTemplateBuilder()
                    .interceptors(consumerIdClientInterceptor,
                                  bearerTokenClientInterceptor,
                                  MdcValuesPropagatingClientInterceptor())
                    .additionalMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
                    .build()
        }
    }

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate(listOf(StringHttpMessageConverter(StandardCharsets.UTF_8),
                                   ByteArrayHttpMessageConverter(),
                                   MappingJackson2HttpMessageConverter(objectMapper)))
    }

    @Bean
    fun restOperations(consumerIdClientInterceptor: ConsumerIdClientInterceptor,
                       mdcValuesPropagatingClientInterceptor: MdcValuesPropagatingClientInterceptor): RestOperations {
        return RestTemplateBuilder()
                .interceptors(consumerIdClientInterceptor, mdcValuesPropagatingClientInterceptor)
                .additionalMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
                .build()
    }

    @Bean
    fun restTemplateBuilderMedProxy(consumerIdClientInterceptor: ConsumerIdClientInterceptor,
                                    mdcValuesPropagatingClientInterceptor: MdcValuesPropagatingClientInterceptor): RestTemplateBuilder {
        val restTemplateBuilder = RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .additionalInterceptors(consumerIdClientInterceptor, mdcValuesPropagatingClientInterceptor)

        return if (trengerProxy()) {
            restTemplateBuilder
                    .additionalCustomizers(NaisProxyCustomizer())
        } else {
            restTemplateBuilder
        }
    }

    private fun trengerProxy(): Boolean {
        return !environment.activeProfiles.any {
            listOf("e2e", "dev", "postgres").contains(it.trim(' '))
        }
    }


}
