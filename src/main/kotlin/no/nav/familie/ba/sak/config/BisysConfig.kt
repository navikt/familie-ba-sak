package no.nav.familie.ba.sak.config

import no.nav.familie.sikkerhet.OIDCUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Configuration
class BisysConfig(
    private val oidcUtil: OIDCUtil,
    @Value("\${BISYS_CLIENT_ID:dummy}")
    private val bisysClientId: String
) {

    @Bean
    fun bisysFilter() = object : OncePerRequestFilter() {
        override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain
        ) {
            if (bisysClientId == oidcUtil.getClaim("azp") && !request.requestURI.startsWith("/api/bisys")) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Kun autorisert for kall mot /api/bisys*")
            } else {
                filterChain.doFilter(request, response)
            }
        }

        override fun shouldNotFilter(request: HttpServletRequest) =
            request.requestURI.contains("/internal") ||
                request.requestURI.startsWith("/swagger") ||
                request.requestURI.startsWith("/v2") // i bruk av swagger
    }
}
