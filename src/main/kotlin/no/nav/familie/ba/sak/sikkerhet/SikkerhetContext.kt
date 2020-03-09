package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder

object SikkerhetContext {
    fun hentSaksbehandler(): String {
        return Result.runCatching { SpringTokenValidationContextHolder().tokenValidationContext }
                .fold(
                        onSuccess = { it.getClaims("azuread")?.get("preferred_username")?.toString() ?: "VL" },
                        onFailure = { "VL" }
                )
    }

    // TODO må hente rolle fra token hvis vi ikke er i systemcontext
    fun hentBehandlerRolle(): BehandlerRolle {
        return if (hentSaksbehandler() == "VL") BehandlerRolle.SYSTEM else BehandlerRolle.SAKSBEHANDLER
    }
}