package no.nav.familie.ba.sak.common.http.interceptor

import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Import(RolleConfig::class)
class RolletilgangInterceptor(private val rolleConfig: RolleConfig) : HandlerInterceptorAdapter() {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val høyesteRolletilgang = SikkerhetContext.hentHøyesteRolletilgangForInnloggetBruker(rolleConfig)

        return høyesteRolletilgang
                       .takeIf {
                           harSkrivetilgang(høyesteRolletilgang) || (harLeseTilgang(høyesteRolletilgang) && request.method == RequestMethod.GET.name)
                       }
                       ?.let { super.preHandle(request, response, handler) }
               ?: run {
                   throw RolleTilgangskontrollFeil(
                           melding = "Autentisert, men ${SikkerhetContext.hentSaksbehandlerNavn()} med rolle $høyesteRolletilgang har ikke tilgang til ${request.method} mot ${request.requestURI}.",
                           frontendFeilmelding = "Du har ikke tilgang til å gjøre denne handlingen."
                   )
               }
    }

    private fun harLeseTilgang(høyesteRolletilgang: BehandlerRolle) =
            høyesteRolletilgang.nivå >= BehandlerRolle.VEILEDER.nivå

    private fun harSkrivetilgang(høyesteRolletilgang: BehandlerRolle) =
            høyesteRolletilgang.nivå >= BehandlerRolle.SAKSBEHANDLER.nivå
}
