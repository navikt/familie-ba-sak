package no.nav.familie.ba.sak.config

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.restklient.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.restklient.interceptor.MdcValuesPropagatingClientInterceptor
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets
import java.time.Duration

@TestConfiguration
@Import(
    ConsumerIdClientInterceptor::class,
    MdcValuesPropagatingClientInterceptor::class,
)
@Profile("mock-rest-template-config")
class RestTemplateTestConfig {
    @Bean
    fun restTemplate(): RestTemplate =
        RestTemplate(
            listOf(
                StringHttpMessageConverter(StandardCharsets.UTF_8),
                ByteArrayHttpMessageConverter(),
                MappingJackson2HttpMessageConverter(objectMapper),
            ),
        )

    @Bean
    fun restOperations(
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
        mdcValuesPropagatingClientInterceptor: MdcValuesPropagatingClientInterceptor,
    ): RestOperations =
        RestTemplateBuilder()
            .interceptors(consumerIdClientInterceptor, mdcValuesPropagatingClientInterceptor)
            .additionalMessageConverters(
                ByteArrayHttpMessageConverter(),
                MappingJackson2HttpMessageConverter(objectMapper),
            ).build()

    @Bean("jwtBearerClientCredentials")
    fun restTemplateJwtBearerClientCredentials(
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
        mdcValuesPropagatingClientInterceptor: MdcValuesPropagatingClientInterceptor,
    ): RestOperations =
        RestTemplateBuilder()
            .additionalInterceptors(consumerIdClientInterceptor, mdcValuesPropagatingClientInterceptor)
            .additionalMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
            .build()

    @Bean("jwtBearer")
    fun restTemplateJwtBearer(
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
        mdcValuesPropagatingClientInterceptor: MdcValuesPropagatingClientInterceptor,
    ): RestOperations =
        RestTemplateBuilder()
            .additionalInterceptors(consumerIdClientInterceptor, mdcValuesPropagatingClientInterceptor)
            .additionalMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
            .build()

    @Bean("jwtBearerMedLangTimeout")
    fun restTemplateJwtBearerMedLangTimeout(
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
        mdcValuesPropagatingClientInterceptor: MdcValuesPropagatingClientInterceptor,
    ): RestOperations =
        RestTemplateBuilder()
            .additionalInterceptors(consumerIdClientInterceptor, mdcValuesPropagatingClientInterceptor)
            .additionalMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
            .build()

    @Bean
    fun restTemplateBuilderMedProxy(
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
        mdcValuesPropagatingClientInterceptor: MdcValuesPropagatingClientInterceptor,
    ): RestTemplateBuilder =
        RestTemplateBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .additionalInterceptors(consumerIdClientInterceptor, mdcValuesPropagatingClientInterceptor)
            .readTimeout(Duration.ofSeconds(5))
}
