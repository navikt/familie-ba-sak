package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder

object SikkerhetContext {

    const val SYSTEM_FORKORTELSE = "VL"
    const val SYSTEM_NAVN = "System"

    /**
     * @param strict hvis true - skal kaste feil hvis token ikke inneholder saksbehandler
     */
    fun hentSaksbehandler(strict: Boolean = false): String {
        val saksbehandler = Result.runCatching { SpringTokenValidationContextHolder().tokenValidationContext }
            .fold(
                onSuccess = { it.getClaims("azuread")?.get("preferred_username")?.toString() ?: SYSTEM_FORKORTELSE },
                onFailure = { SYSTEM_FORKORTELSE }
            )

        if (strict && saksbehandler == SYSTEM_FORKORTELSE) {
            error("Finner ikke NAVident i token")
        }
        return saksbehandler
    }

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

    fun hentRolletilgangFraSikkerhetscontext(
        rolleConfig: RolleConfig,
        lavesteSikkerhetsnivå: BehandlerRolle?
    ): BehandlerRolle {
        if (hentSaksbehandler() == SYSTEM_FORKORTELSE) return BehandlerRolle.SYSTEM

        val grupper = hentGrupper()
        val høyesteSikkerhetsnivåForInnloggetBruker: BehandlerRolle =
            when {
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
        return when {
            grupper.contains(rolleConfig.BESLUTTER_ROLLE) -> BehandlerRolle.BESLUTTER
            grupper.contains(rolleConfig.SAKSBEHANDLER_ROLLE) -> BehandlerRolle.SAKSBEHANDLER
            grupper.contains(rolleConfig.VEILEDER_ROLLE) -> BehandlerRolle.VEILEDER
            else -> BehandlerRolle.UKJENT
        }
    }
}
