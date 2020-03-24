package no.nav.familie.ba.sak.config

import no.nav.familie.http.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.http.interceptor.MdcValuesPropagatingClientInterceptor
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets
import java.time.Duration

@Configuration
@Import(ConsumerIdClientInterceptor::class, MdcValuesPropagatingClientInterceptor::class)
@Profile("integrasjonstest")
class RestTemplateConfig {

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate(listOf(StringHttpMessageConverter(StandardCharsets.UTF_8), ByteArrayHttpMessageConverter()))
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
                .additionalCustomizers(NaisProxyCustomizer())
                .additionalInterceptors(consumerIdClientInterceptor, mdcValuesPropagatingClientInterceptor)
                .additionalMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
                .build()
    }
}