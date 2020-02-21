package no.nav.familie.ba.sak.config

import no.nav.familie.http.interceptor.InternLoggerInterceptor
import no.nav.familie.sikkerhet.OIDCUtil
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
        private val oidcUtil: OIDCUtil
): WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(InternLoggerInterceptor(oidcUtil))
        super.addInterceptors(registry)
    }
}