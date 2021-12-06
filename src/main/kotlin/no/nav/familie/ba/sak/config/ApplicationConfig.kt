package no.nav.familie.ba.sak.config

import no.nav.familie.log.filter.LogFilter
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.retry.annotation.EnableRetry
import org.springframework.util.ReflectionUtils
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping
import springfox.documentation.spring.web.plugins.WebFluxRequestHandlerProvider
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider
import java.lang.reflect.Field
import java.util.stream.Collectors

@SpringBootConfiguration
@EntityScan("no.nav.familie.prosessering", ApplicationConfig.PAKKENAVN)
@ComponentScan("no.nav.familie.prosessering", ApplicationConfig.PAKKENAVN)
@EnableRetry
@ConfigurationPropertiesScan
@EnableJwtTokenValidation(ignore = ["springfox.documentation.swagger"])
@EnableOAuth2Client(cacheEnabled = true)
class ApplicationConfig {

    // Denne bønna er en workaround etter bump av spring-boot-starter-parent til 2.6.x, som da ikke er helt venn med
    // gjeldende versjon av springfox-boot-starter (3.0.0 i skrivende stund). Når dette løses kan denne fjernes.
    // Løsningen er ihht anbefalingen her: https://github.com/springfox/springfox/issues/3462#issuecomment-983144080
    @Bean
    fun springfoxHandlerProviderBeanPostProcessor(): BeanPostProcessor? {
        return object : BeanPostProcessor {
            @Throws(BeansException::class)
            override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
                if (bean is WebMvcRequestHandlerProvider || bean is WebFluxRequestHandlerProvider) {
                    customizeSpringfoxHandlerMappings<RequestMappingInfoHandlerMapping>(getHandlerMappings(bean))
                }
                return bean
            }

            private fun <T : RequestMappingInfoHandlerMapping?> customizeSpringfoxHandlerMappings(mappings: MutableList<T>) {
                val copy = mappings.stream()
                    .filter { mapping: T -> mapping!!.patternParser == null }
                    .collect(Collectors.toList())
                mappings.clear()
                mappings.addAll(copy)
            }

            private fun getHandlerMappings(bean: Any): MutableList<RequestMappingInfoHandlerMapping> {
                return try {
                    val field: Field = ReflectionUtils.findField(bean.javaClass, "handlerMappings")!!
                    field.setAccessible(true)
                    field.get(bean) as MutableList<RequestMappingInfoHandlerMapping>
                } catch (e: IllegalArgumentException) {
                    throw IllegalStateException(e)
                } catch (e: IllegalAccessException) {
                    throw IllegalStateException(e)
                }
            }
        }
    }

    @Bean
    fun logFilter(): FilterRegistrationBean<LogFilter> {
        log.info("Registering LogFilter filter")
        val filterRegistration: FilterRegistrationBean<LogFilter> = FilterRegistrationBean()
        filterRegistration.filter = LogFilter()
        filterRegistration.order = 1
        return filterRegistration
    }

    companion object {

        private val log = LoggerFactory.getLogger(ApplicationConfig::class.java)
        const val PAKKENAVN = "no.nav.familie.ba.sak"
    }
}
