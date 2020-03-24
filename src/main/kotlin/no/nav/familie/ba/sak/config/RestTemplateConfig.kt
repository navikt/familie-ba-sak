package no.nav.familie.ba.sak.config

import no.nav.familie.http.interceptor.BearerTokenClientInterceptor
import no.nav.familie.http.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.http.interceptor.MdcValuesPropagatingClientInterceptor
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets
import java.time.Duration

@Configuration
@Import(ConsumerIdClientInterceptor::class, BearerTokenClientInterceptor::class, MdcValuesPropagatingClientInterceptor::class)
class RestTemplateConfig(
        private val environment: Environment
) {

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
        return RestTemplate(listOf(StringHttpMessageConverter(StandardCharsets.UTF_8), ByteArrayHttpMessageConverter()))
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
            listOf("e2e", "dev").contains(it.trim(' '))
        }
    }
}
