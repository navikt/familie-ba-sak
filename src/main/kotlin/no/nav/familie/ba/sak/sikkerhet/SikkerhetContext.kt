package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.config.BehandlerRolle
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

    fun harInnloggetBrukerForvalterRolle(): Boolean = harRolle(Rolle.FORVALTER)

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

    fun hentHøyesteRolletilgangForInnloggetBruker(): BehandlerRolle =
        when {
            erSystemKontekst() -> BehandlerRolle.SYSTEM
            harRolle(Rolle.BESLUTTER) -> BehandlerRolle.BESLUTTER
            harRolle(Rolle.SAKSBEHANDLER) -> BehandlerRolle.SAKSBEHANDLER
            harRolle(Rolle.FORVALTER) -> BehandlerRolle.FORVALTER
            harRolle(Rolle.VEILEDER) -> BehandlerRolle.VEILEDER
            else -> BehandlerRolle.UKJENT
        }

    fun hentGrupper(): List<String> = hentClaimFraToken("groups") ?: emptyList()

    fun harRolle(rolle: Rolle): Boolean = hentJwtAuthenticationToken()?.authorities?.any { it.authority == rolle.authority() } == true

    fun <T> hentClaimFraToken(claim: String): T? = runCatching { hentJwt()?.getClaim<T>(claim) }.getOrNull()

    fun hentJwt(): Jwt? = hentJwtAuthenticationToken()?.token

    private fun hentJwtAuthenticationToken(): JwtAuthenticationToken? = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
}
