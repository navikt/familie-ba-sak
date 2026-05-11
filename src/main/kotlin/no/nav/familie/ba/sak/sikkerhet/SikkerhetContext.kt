package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.RolleConfig
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

object SikkerhetContext {
    const val SYSTEM_FORKORTELSE = "VL"
    const val SYSTEM_NAVN = "System"

    fun erSystemKontekst() = hentSaksbehandler() == SYSTEM_FORKORTELSE

    fun erMaskinTilMaskinToken(): Boolean {
        val oid = hentClaimFraToken<String>("oid")
        val sub = hentClaimFraToken<String>("sub")
        val roles = hentClaimFraToken<List<String>>("roles").orEmpty()
        return oid != null && oid == sub && roles.contains("access_as_application")
    }

    fun harInnloggetBrukerForvalterRolle(rolleConfig: RolleConfig): Boolean = hentGrupper().contains(rolleConfig.FORVALTER_ROLLE)

    fun hentSaksbehandler(): String = hentClaimFraToken("NAVident") ?: SYSTEM_FORKORTELSE

    fun hentSaksbehandlerEpost(): String = hentClaimFraToken("preferred_username") ?: SYSTEM_FORKORTELSE

    fun hentSaksbehandlerNavn(): String = hentClaimFraToken("name") ?: SYSTEM_NAVN

    fun hentRolletilgangFraSikkerhetscontext(lavesteSikkerhetsnivå: BehandlerRolle?): BehandlerRolle {
        val høyeste = hentHøyesteRolletilgangForInnloggetBruker()
        return when {
            høyeste == BehandlerRolle.SYSTEM -> BehandlerRolle.SYSTEM
            lavesteSikkerhetsnivå == null -> BehandlerRolle.UKJENT
            høyeste.nivå >= lavesteSikkerhetsnivå.nivå -> lavesteSikkerhetsnivå
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

    fun hentGrupper(): List<String> = hentClaimFraToken("groups") ?: emptyList()

    fun <T> hentClaimFraToken(claim: String): T? = runCatching { hentJwt()?.getClaim<T>(claim) }.getOrNull()

    fun hentJwt(): Jwt? = hentJwtAuthenticationToken()?.token

    private fun hentJwtAuthenticationToken(): JwtAuthenticationToken? = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken

    fun kallKommerFraKlage(): Boolean = kallKommerFra("teamfamilie:familie-klage")

    private fun kallKommerFra(forventetApplikasjonsSuffix: String): Boolean {
        val applikasjonsnavn = hentClaimFraToken<String>("azp_name") ?: "" // e.g. dev-gcp:some-team:application-name
        secureLogger.info("Applikasjonsnavn: $applikasjonsnavn")
        return applikasjonsnavn.endsWith(forventetApplikasjonsSuffix)
    }
}
