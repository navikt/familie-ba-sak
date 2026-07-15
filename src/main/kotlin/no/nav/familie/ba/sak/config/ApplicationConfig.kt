package no.nav.familie.ba.sak.config

import no.nav.familie.log.NavSystemtype
import no.nav.familie.log.filter.LogFilter
import no.nav.familie.log.filter.RequestTimeFilter
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.resilience.annotation.EnableResilientMethods

@SpringBootConfiguration
@EntityScan("no.nav.familie.prosessering", ApplicationConfig.PAKKENAVN)
@ComponentScan(
    "no.nav.familie.prosessering",
    "no.nav.familie.unleash",
    ApplicationConfig.PAKKENAVN,
    "no.nav.familie.metrikker",
    "no.nav.familie.felles.tokenklient.entraid",
    "no.nav.familie.felles.tokenklient.tokenx",
)
@EnableResilientMethods
@ConfigurationPropertiesScan
@Import(no.nav.familie.sikkerhet.context.FamilieFellesSpringSecurityKonfigurasjon::class)
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

    companion object {
        private val log = LoggerFactory.getLogger(ApplicationConfig::class.java)
        const val PAKKENAVN = "no.nav.familie.ba.sak"
    }
}
