package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.sikkerhet.TokenXAuthenticationManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfiguration(
    private val tokenXAuthenticationManager: TokenXAuthenticationManager,
) {
    @Bean
    @Order(1)
    fun publicSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            securityMatcher("/internal/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
            authorizeHttpRequests {
                authorize(anyRequest, permitAll)
            }
        }
        return http.build()
    }

    @Bean
    @Order(2)
    fun tokenXSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            securityMatcher("/api/minside/**")
            authorizeHttpRequests {
                authorize(anyRequest, authenticated)
            }
            oauth2ResourceServer {
                jwt {
                    authenticationManager = tokenXAuthenticationManager
                }
            }
        }
        return http.build()
    }
}
