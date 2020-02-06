package no.nav.familie.ba.sak.config

import no.nav.familie.http.interceptor.ClientCredentialsInterceptor
import no.nav.familie.http.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.http.interceptor.JwtBearerInterceptor
import no.nav.familie.http.interceptor.MdcValuesPropagatingClientInterceptor
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestOperations
import java.time.Duration

@Configuration
@Import(ConsumerIdClientInterceptor::class)
@Profile("!dev")
@Import(ConsumerIdClientInterceptor::class, ClientCredentialsInterceptor::class, JwtBearerInterceptor::class)
class RestTemplateConfig {

    @Bean("jwtBearer")
    fun restTemplateJwtBearer(consumerIdClientInterceptor: ConsumerIdClientInterceptor,
                              jwtBearerInterceptor: JwtBearerInterceptor): RestOperations {

        return RestTemplateBuilder()
                .additionalCustomizers(NaisProxyCustomizer())
                .interceptors(consumerIdClientInterceptor,
                              jwtBearerInterceptor,
                              MdcValuesPropagatingClientInterceptor())
                .build()
    }

    @Bean("clientCredentials")
    fun restTemplateClientCredentials(consumerIdClientInterceptor: ConsumerIdClientInterceptor,
                                      clientCredentialsInterceptor: ClientCredentialsInterceptor): RestOperations {
        return RestTemplateBuilder()
                .additionalCustomizers(NaisProxyCustomizer())
                .interceptors(consumerIdClientInterceptor,
                              clientCredentialsInterceptor,
                              MdcValuesPropagatingClientInterceptor())
                .build()
    }

    @Bean
    fun restTemplateBuilderMedProxy(consumerIdClientInterceptor: ConsumerIdClientInterceptor): RestTemplateBuilder? {
        return RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .additionalInterceptors(consumerIdClientInterceptor)
                .additionalCustomizers(NaisProxyCustomizer())
    }
}
