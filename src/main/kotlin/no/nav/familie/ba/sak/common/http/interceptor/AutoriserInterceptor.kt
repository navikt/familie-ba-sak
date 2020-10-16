package no.nav.familie.ba.sak.common.http.interceptor

import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Import(RolleConfig::class)
class AutoriserInterceptor(private val rolleConfig: RolleConfig) : HandlerInterceptorAdapter() {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean =
            SikkerhetContext.hentBehandlerRolleForSteg(rolleConfig, BehandlerRolle.VEILEDER)
                    .takeIf { it != BehandlerRolle.UKJENT }
                    ?.let { super.preHandle(request, response, handler) }
            ?: run {
                LOG.info("Bruker ${SikkerhetContext.hentSaksbehandler()} har ikke tilgang.")
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bruker har ikke tilgang")
                false
            }

    companion object {

        private val LOG = LoggerFactory.getLogger(AutoriserInterceptor::class.java)
    }
}
