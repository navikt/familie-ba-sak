package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.common.http.interceptor.AutoriserInterceptor
import no.nav.familie.http.interceptor.InternLoggerInterceptor
import no.nav.familie.sikkerhet.OIDCUtil
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@Import(*[OIDCUtil::class, RolleConfig::class])
class WebConfig(
        private val oidcUtil: OIDCUtil,
        private val rolleConfig: RolleConfig
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(InternLoggerInterceptor(oidcUtil))
        registry.addInterceptor(AutoriserInterceptor(rolleConfig))
        super.addInterceptors(registry)
    }
}