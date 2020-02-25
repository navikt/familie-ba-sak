package no.nav.familie.ba.sak.sikkerhet

import no.nav.security.token.support.spring.SpringTokenValidationContextHolder

object SikkerhetContext {
    fun hentSaksbehandler(): String {
        val tokenValidationContext = SpringTokenValidationContextHolder().tokenValidationContext
        return tokenValidationContext?.getClaims("azuread")?.get("preferred_username")?.toString() ?: "VL"
    }
}