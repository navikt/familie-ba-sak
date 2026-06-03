package no.nav.familie.ba.sak.config

import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.log.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.log.interceptor.MdcValuesPropagatingClientInterceptor
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets

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
                JacksonJsonHttpMessageConverter(jsonMapper),
            ),
        )
}
