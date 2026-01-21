package no.nav.familie.ba.sak.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.familie.log.NavSystemtype
import no.nav.familie.log.filter.LogFilter
import no.nav.familie.log.filter.RequestTimeFilter
import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import no.nav.familie.restklient.client.RetryOAuth2HttpClient
import no.nav.security.token.support.client.core.http.OAuth2HttpClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.retry.annotation.EnableRetry
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.time.temporal.ChronoUnit

@SpringBootConfiguration
@EntityScan("no.nav.familie.prosessering", ApplicationConfig.PAKKENAVN)
@ComponentScan("no.nav.familie.prosessering", "no.nav.familie.unleash", ApplicationConfig.PAKKENAVN, "no.nav.familie.metrikker")
@EnableRetry
@ConfigurationPropertiesScan
@EnableJwtTokenValidation(ignore = ["org.springdoc"])
@EnableOAuth2Client(cacheEnabled = true)
class ApplicationConfig {
    @Bean
    fun logFilter(): FilterRegistrationBean<LogFilter> {
        log.info("Registering LogFilter filter")
        val filterRegistration: FilterRegistrationBean<LogFilter> = FilterRegistrationBean()
        filterRegistration.setFilter(LogFilter(NavSystemtype.NAV_SAKSBEHANDLINGSSYSTEM))
        filterRegistration.order = 1
        return filterRegistration
    }

    @Bean
    fun requestTimeFilter(): FilterRegistrationBean<RequestTimeFilter> {
        log.info("Registering RequestTimeFilter")
        val filterRegistration = FilterRegistrationBean<RequestTimeFilter>()
        filterRegistration.setFilter(RequestTimeFilter())
        filterRegistration.order = 2
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
            .connectTimeout(Duration.of(2, ChronoUnit.SECONDS))
            .readTimeout(Duration.of(30, ChronoUnit.SECONDS))
            .additionalMessageConverters(listOf(jackson2HttpMessageConverter) + RestTemplate().messageConverters)
    }

    /**
     * Overskriver OAuth2HttpClient som settes opp i token-support som ikke kan få med objectMapper fra felles
     * pga. .setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
     * og [OAuth2AccessTokenResponse] som burde settes med setters, då feltnavn heter noe annet enn feltet i json
     */
    @Bean
    @Primary
    fun oAuth2HttpClient(): OAuth2HttpClient =
        RetryOAuth2HttpClient(
            RestClient.create(
                RestTemplateBuilder()
                    .connectTimeout(Duration.of(2, ChronoUnit.SECONDS))
                    .readTimeout(Duration.of(4, ChronoUnit.SECONDS))
                    .build(),
            ),
        )

    @Bean
    fun prosesseringInfoProvider(
        @Value("\${prosessering.rolle}") prosesseringRolle: String,
    ) = object : ProsesseringInfoProvider {
        override fun hentBrukernavn(): String =
            try {
                SpringTokenValidationContextHolder().getTokenValidationContext().getClaims("azuread").getStringClaim("preferred_username")
            } catch (e: Exception) {
                "VL"
            }

        override fun harTilgang(): Boolean = grupper().contains(prosesseringRolle)

        @Suppress("UNCHECKED_CAST")
        private fun grupper(): List<String> =
            try {
                SpringTokenValidationContextHolder()
                    .getTokenValidationContext()
                    .getClaims("azuread")
                    ?.get("groups") as List<String>? ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ApplicationConfig::class.java)
        const val PAKKENAVN = "no.nav.familie.ba.sak"
    }
}
