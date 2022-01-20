package no.nav.familie.ba.sak.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.familie.log.filter.LogFilter
import no.nav.security.token.support.client.core.http.OAuth2HttpClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.spring.oauth2.DefaultOAuth2HttpClient
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.retry.annotation.EnableRetry
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.time.temporal.ChronoUnit

@SpringBootConfiguration
@EntityScan("no.nav.familie.prosessering", ApplicationConfig.PAKKENAVN)
@ComponentScan("no.nav.familie.prosessering", ApplicationConfig.PAKKENAVN)
@EnableRetry
@ConfigurationPropertiesScan
@EnableJwtTokenValidation(ignore = ["org.springdoc"])
@EnableOAuth2Client(cacheEnabled = true)
class ApplicationConfig {

    @Bean
    fun logFilter(): FilterRegistrationBean<LogFilter> {
        log.info("Registering LogFilter filter")
        val filterRegistration: FilterRegistrationBean<LogFilter> = FilterRegistrationBean()
        filterRegistration.filter = LogFilter()
        filterRegistration.order = 1
        return filterRegistration
    }

    /**
     * Overskriver felles sin som bruker proxy, som ikke skal brukes på gcp.
     */
    @Bean
    @Primary
    fun restTemplateBuilder(objectMapper: ObjectMapper): RestTemplateBuilder {
        val jackson2HttpMessageConverter = MappingJackson2HttpMessageConverter(objectMapper)
        return RestTemplateBuilder()
            .setConnectTimeout(Duration.of(2, ChronoUnit.SECONDS))
            .setReadTimeout(Duration.of(30, ChronoUnit.SECONDS))
            .additionalMessageConverters(listOf(jackson2HttpMessageConverter) + RestTemplate().messageConverters)
    }

    /**
     * Overskriver OAuth2HttpClient som settes opp i token-support som ikke kan få med objectMapper fra felles
     * pga. .setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
     * og [OAuth2AccessTokenResponse] som burde settes med setters, då feltnavn heter noe annet enn feltet i json
     */
    @Bean
    @Primary
    fun oAuth2HttpClient(): OAuth2HttpClient {
        return DefaultOAuth2HttpClient(
            RestTemplateBuilder()
                .setConnectTimeout(Duration.of(2, ChronoUnit.SECONDS))
                .setReadTimeout(Duration.of(4, ChronoUnit.SECONDS))
        )
    }

    companion object {

        private val log = LoggerFactory.getLogger(ApplicationConfig::class.java)
        const val PAKKENAVN = "no.nav.familie.ba.sak"
    }
}
