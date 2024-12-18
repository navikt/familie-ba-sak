package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder

object SikkerhetContext {
    const val SYSTEM_FORKORTELSE = "VL"
    const val SYSTEM_NAVN = "System"

    fun erSystemKontekst() = hentSaksbehandler() == SYSTEM_FORKORTELSE

    fun erMaskinTilMaskinToken(): Boolean {
        val claims = SpringTokenValidationContextHolder().getTokenValidationContext().getClaims("azuread")
        return claims.get("oid") != null &&
            claims.get("oid") == claims.get("sub") &&
            claims.getAsList("roles").contains("access_as_application")
    }

    fun harInnloggetBrukerForvalterRolle(rolleConfig: RolleConfig): Boolean = hentGrupper().contains(rolleConfig.FORVALTER_ROLLE)

    fun hentSaksbehandler(): String =
        Result
            .runCatching { SpringTokenValidationContextHolder().getTokenValidationContext() }
            .fold(
                onSuccess = {
                    it.hentClaimsForIssuer("azuread")?.get("NAVident")?.toString() ?: SYSTEM_FORKORTELSE
                },
                onFailure = { SYSTEM_FORKORTELSE },
            )

    fun hentSaksbehandlerEpost(): String =
        Result
            .runCatching { SpringTokenValidationContextHolder().getTokenValidationContext() }
            .fold(
                onSuccess = { it.hentClaimsForIssuer("azuread")?.get("preferred_username")?.toString() ?: SYSTEM_FORKORTELSE },
                onFailure = { SYSTEM_FORKORTELSE },
            )

    fun hentSaksbehandlerNavn(): String =
        Result
            .runCatching { SpringTokenValidationContextHolder().getTokenValidationContext() }
            .fold(
                onSuccess = { it.hentClaimsForIssuer("azuread")?.get("name")?.toString() ?: SYSTEM_NAVN },
                onFailure = { SYSTEM_NAVN },
            )

    fun hentRolletilgangFraSikkerhetscontext(
        rolleConfig: RolleConfig,
        lavesteSikkerhetsnivå: BehandlerRolle?,
    ): BehandlerRolle {
        if (hentSaksbehandler() == SYSTEM_FORKORTELSE) return BehandlerRolle.SYSTEM

        val grupper = hentGrupper()
        val høyesteSikkerhetsnivåForInnloggetBruker: BehandlerRolle =
            when {
                grupper.contains(rolleConfig.BESLUTTER_ROLLE) -> BehandlerRolle.BESLUTTER
                grupper.contains(rolleConfig.SAKSBEHANDLER_ROLLE) -> BehandlerRolle.SAKSBEHANDLER
                grupper.contains(rolleConfig.FORVALTER_ROLLE) -> BehandlerRolle.FORVALTER
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
            grupper.contains(rolleConfig.FORVALTER_ROLLE) -> BehandlerRolle.FORVALTER
            grupper.contains(rolleConfig.VEILEDER_ROLLE) -> BehandlerRolle.VEILEDER
            else -> BehandlerRolle.UKJENT
        }
    }

    fun hentGrupper(): List<String> =
        Result
            .runCatching { SpringTokenValidationContextHolder().getTokenValidationContext() }
            .fold(
                onSuccess = {
                    @Suppress("UNCHECKED_CAST")
                    it.hentClaimsForIssuer("azuread")?.get("groups") as List<String>? ?: emptyList()
                },
                onFailure = { emptyList() },
            )

    fun TokenValidationContext.hentClaimsForIssuer(issuer: String): JwtTokenClaims? = if (this.issuers.contains(issuer)) this.getClaims(issuer) else null

    fun kallKommerFraKlage(): Boolean = kallKommerFra("teamfamilie:familie-klage")

    private fun kallKommerFra(forventetApplikasjonsSuffix: String): Boolean {
        val claims = SpringTokenValidationContextHolder().getTokenValidationContext().getClaims("azuread")
        val applikasjonsnavn = claims.get("azp_name")?.toString() ?: "" // e.g. dev-gcp:some-team:application-name
        secureLogger.info("Applikasjonsnavn: $applikasjonsnavn")
        return applikasjonsnavn.endsWith(forventetApplikasjonsSuffix)
    }
}
