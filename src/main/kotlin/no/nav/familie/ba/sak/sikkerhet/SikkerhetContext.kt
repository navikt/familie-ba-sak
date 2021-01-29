package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder

object SikkerhetContext {

    const val SYSTEM_FORKORTELSE = "VL"
    const val SYSTEM_NAVN = "System"

    fun hentSaksbehandler(): String {
        return Result.runCatching { SpringTokenValidationContextHolder().tokenValidationContext }
                .fold(
                        onSuccess = { it.getClaims("azuread")?.get("preferred_username")?.toString() ?: SYSTEM_FORKORTELSE },
                        onFailure = { SYSTEM_FORKORTELSE }
                )
    }

    fun erSystemKontekst() = hentSaksbehandler() == SYSTEM_FORKORTELSE

    fun hentSaksbehandlerNavn(): String {
        return Result.runCatching { SpringTokenValidationContextHolder().tokenValidationContext }
                .fold(
                        onSuccess = { it.getClaims("azuread")?.get("name")?.toString() ?: SYSTEM_NAVN },
                        onFailure = { SYSTEM_NAVN }
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

    fun hentRolletilgangFraSikkerhetscontext(rolleConfig: RolleConfig, lavesteSikkerhetsnivå: BehandlerRolle?): BehandlerRolle {
        if (hentSaksbehandler() == SYSTEM_FORKORTELSE) return BehandlerRolle.SYSTEM

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

    fun hentHøyesteRolletilgangForInnloggetBruker(rolleConfig: RolleConfig): BehandlerRolle {
        if (hentSaksbehandler() == SYSTEM_FORKORTELSE) return BehandlerRolle.SYSTEM

        val grupper = hentGrupper()
        return if (rolleConfig.ENVIRONMENT_NAME == "local") BehandlerRolle.BESLUTTER else when {
            grupper.contains(rolleConfig.BESLUTTER_ROLLE) -> BehandlerRolle.BESLUTTER
            grupper.contains(rolleConfig.SAKSBEHANDLER_ROLLE) -> BehandlerRolle.SAKSBEHANDLER
            grupper.contains(rolleConfig.VEILEDER_ROLLE) -> BehandlerRolle.VEILEDER
            else -> BehandlerRolle.UKJENT
        }
    }
}