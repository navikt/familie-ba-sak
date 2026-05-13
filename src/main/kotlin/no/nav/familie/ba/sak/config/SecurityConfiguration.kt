package no.nav.familie.ba.sak.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.familie.ba.sak.sikkerhet.AzureAdAuthenticationManager
import no.nav.familie.ba.sak.sikkerhet.Rolle.BISYS_APPLIKASJON
import no.nav.familie.ba.sak.sikkerhet.Rolle.Companion.rollerMedInternTilgang
import no.nav.familie.ba.sak.sikkerhet.Rolle.KLAGE_APPLIKASJON
import no.nav.familie.ba.sak.sikkerhet.Rolle.PENSJON_APPLIKASJON
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TokenXAuthenticationManager
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Ressurs.Companion.failure
import no.nav.familie.kontrakter.felles.jsonMapper
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfiguration(
    private val tokenXAuthenticationManager: TokenXAuthenticationManager,
    private val azureAdAuthenticationManager: AzureAdAuthenticationManager,
) {
    private val logger = LoggerFactory.getLogger(SecurityConfiguration::class.java)

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
            exceptionHandling {
                accessDeniedHandler = accessDeniedHandler()
                authenticationEntryPoint = authenticationEntryPoint()
            }
        }
        return http.build()
    }

    @Bean
    @Order(3)
    fun azureAdSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            authorizeHttpRequests {
                authorize("/api/ekstern/pensjon/**", hasRole(PENSJON_APPLIKASJON.name))
                authorize("/api/bisys/**", hasRole(BISYS_APPLIKASJON.name))
                authorize("/api/klage/**", hasRole(KLAGE_APPLIKASJON.name))
                authorize(anyRequest, hasAnyRole(*rollerMedInternTilgang()))
            }
            oauth2ResourceServer {
                jwt {
                    authenticationManager = azureAdAuthenticationManager
                }
            }
            exceptionHandling {
                accessDeniedHandler = accessDeniedHandler()
                authenticationEntryPoint = authenticationEntryPoint()
            }
        }
        return http.build()
    }

    private fun accessDeniedHandler(): AccessDeniedHandler =
        AccessDeniedHandler { _: HttpServletRequest, response: HttpServletResponse, _: AccessDeniedException ->
            logger.info("Bruker ${SikkerhetContext.hentSaksbehandler()} har ikke tilgang.")
            response.apply {
                status = HttpServletResponse.SC_FORBIDDEN
                contentType = MediaType.APPLICATION_JSON_VALUE
                characterEncoding = Charsets.UTF_8.name()
                jsonMapper.writeValue(
                    writer,
                    Ressurs(
                        data = null,
                        status = Ressurs.Status.IKKE_TILGANG,
                        melding = "Bruker har ikke tilgang til saksbehandlingsløsningen",
                        frontendFeilmelding = "Du mangler tilgang til denne saksbehandlingsløsningen",
                        stacktrace = null,
                    ),
                )
            }
        }

    private fun authenticationEntryPoint(): AuthenticationEntryPoint =
        AuthenticationEntryPoint { _: HttpServletRequest, response: HttpServletResponse, _: AuthenticationException ->
            response.apply {
                status = HttpServletResponse.SC_UNAUTHORIZED
                contentType = MediaType.APPLICATION_JSON_VALUE
                characterEncoding = Charsets.UTF_8.name()
                jsonMapper.writeValue(
                    writer,
                    failure<Nothing>(
                        errorMessage = "401 Unauthorized",
                        frontendFeilmelding = "Kall ikke autorisert",
                    ),
                )
            }
        }
}
