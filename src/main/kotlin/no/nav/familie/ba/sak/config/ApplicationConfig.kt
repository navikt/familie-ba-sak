package no.nav.familie.ba.sak.config

import no.nav.familie.log.filter.LogFilter
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.retry.annotation.EnableRetry

@SpringBootConfiguration
@EnableJpaAuditing
@EnableJpaRepositories("no.nav.familie.prosessering", ApplicationConfig.pakkenavn)
@EntityScan("no.nav.familie.prosessering", ApplicationConfig.pakkenavn)
@ComponentScan("no.nav.familie.prosessering", ApplicationConfig.pakkenavn)
@ConfigurationPropertiesScan
@EnableRetry
@EnableJwtTokenValidation
@EnableOAuth2Client(cacheEnabled = true)
class ApplicationConfig {

    @Bean
    fun logFilter(): FilterRegistrationBean<LogFilter> {
        log.info("Registering LogFilter filterr")
        val filterRegistration: FilterRegistrationBean<LogFilter> = FilterRegistrationBean()
        filterRegistration.filter = LogFilter()
        filterRegistration.order = 1
        return filterRegistration
    }

    companion object {
        private val log = LoggerFactory.getLogger(ApplicationConfig::class.java)
        const val pakkenavn = "no.nav.familie.ba.sak"
    }
}
