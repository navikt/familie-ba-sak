package no.nav.familie.ba.sak.config

import no.nav.familie.http.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.http.interceptor.MdcValuesPropagatingClientInterceptor
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
@Import(
        ConsumerIdClientInterceptor::class,
        MdcValuesPropagatingClientInterceptor::class)
@Profile("integrasjonstest")
class RestTemplateTestConfig {


    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplateBuilder()
                .additionalInterceptors()
                .additionalMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
                .build()
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

    @Bean("jwtBearerClientCredentials")
    fun restTemplateJwtBearerClientCredentials(consumerIdClientInterceptor: ConsumerIdClientInterceptor,
                                               mdcValuesPropagatingClientInterceptor: MdcValuesPropagatingClientInterceptor)
            : RestOperations {
        return RestTemplateBuilder()
                .additionalInterceptors(consumerIdClientInterceptor, mdcValuesPropagatingClientInterceptor)
                .additionalMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
                .build()
    }

    @Bean("jwtBearer")
    fun restTemplateJwtBearer(consumerIdClientInterceptor: ConsumerIdClientInterceptor,
                              mdcValuesPropagatingClientInterceptor: MdcValuesPropagatingClientInterceptor)
            : RestOperations {
        return RestTemplateBuilder()
                .additionalInterceptors(consumerIdClientInterceptor, mdcValuesPropagatingClientInterceptor)
                .additionalMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
                .build()
    }
}