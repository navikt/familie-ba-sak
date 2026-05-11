package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.hentJwt
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder

// TODO: Fjern denne klassen når alle klienter er skrevet om til å bruke Texas.

/**
 * Midlertidig konfigurasjon i overgangen fra Nav token-support til Spring Security.
 *
 * Eksponerer en [TokenValidationContextHolder]-bønne som leser token fra Spring Securitys
 * [SecurityContextHolder] i stedet for Nav token-support sin egen kontekst.
 *
 * [EnableOAuth2Client][no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client] i [ApplicationConfig] aktiverer
 * `token-client-spring` sin OAuth2-klientinfrastruktur, som internt bruker [TokenValidationContextHolder] til å
 * hente brukerens innkommende token ved on-behalf-of-utveksling mot andre tjenester. Denne bønnen sørger for at
 * det fungerer selv om Nav token-support ellers ikke er aktivt i applikasjonen.
 *
 */
@Configuration
class TokenValidationContextHolderConfig {
    @Bean
    fun tokenValidationContextHolder(): TokenValidationContextHolder = SpringSecurityTokenValidationContextHolder()

    class SpringSecurityTokenValidationContextHolder : TokenValidationContextHolder {
        override fun getTokenValidationContext(): TokenValidationContext {
            val validatedTokens = hentJwt()?.let { mapOf(it.issuer.toString() to JwtToken(it.tokenValue)) }.orEmpty()
            return TokenValidationContext(validatedTokens)
        }

        override fun setTokenValidationContext(tokenValidationContext: TokenValidationContext?) {
            // No-op: Spring Security manages the token context
        }
    }
}
