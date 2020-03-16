package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.slf4j.LoggerFactory

object SikkerhetContext {
    fun hentSaksbehandler(): String {
        return Result.runCatching { SpringTokenValidationContextHolder().tokenValidationContext }
                .fold(
                        onSuccess = { it.getClaims("azuread")?.get("preferred_username")?.toString() ?: "VL" },
                        onFailure = { "VL" }
                )
    }

    fun hentSaksbehandlerNavn(): String {
        return Result.runCatching { SpringTokenValidationContextHolder().tokenValidationContext }
                .fold(
                        onSuccess = { it.getClaims("azuread")?.get("name")?.toString() ?: "System" },
                        onFailure = { "System" }
                )
    }

    fun hentGrupper(): List<String> {
        return Result.runCatching { SpringTokenValidationContextHolder().tokenValidationContext }
                .fold(
                        onSuccess = {
                            @Suppress("UNCHECKED_CAST")
                            it.getClaims("azuread")?.get("groups") as List<String>? ?: emptyList()
                        },
                        onFailure = { emptyList() }
                )
    }

    fun hentBehandlerRolleForSteg(rolleConfig: RolleConfig, lavesteSikkerhetsnivå: BehandlerRolle?): BehandlerRolle {
        if (hentSaksbehandler() == "VL") return BehandlerRolle.SYSTEM

        val grupper = hentGrupper()
        val høyesteSikkerhetsnivåForInnloggetBruker: BehandlerRolle =
                if (rolleConfig.ENVIRONMENT_NAME == "local") BehandlerRolle.BESLUTTER else when {
                    grupper.contains(rolleConfig.BESLUTTER_ROLLE) -> BehandlerRolle.BESLUTTER
                    grupper.contains(rolleConfig.SAKSBEHANDLER_ROLLE) -> BehandlerRolle.SAKSBEHANDLER
                    grupper.contains(rolleConfig.VEILEDER_ROLLE) -> BehandlerRolle.VEILEDER
                    else -> BehandlerRolle.UKJENT
                }

        return when {
            lavesteSikkerhetsnivå == null -> BehandlerRolle.UKJENT
            høyesteSikkerhetsnivåForInnloggetBruker.nivå >= lavesteSikkerhetsnivå.nivå -> lavesteSikkerhetsnivå
            else -> BehandlerRolle.UKJENT
        }
    }
}