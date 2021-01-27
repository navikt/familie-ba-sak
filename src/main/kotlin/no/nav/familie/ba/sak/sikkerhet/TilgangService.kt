package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.RolleConfig
import org.springframework.stereotype.Service

@Service
class TilgangService(
        private val rolleConfig: RolleConfig
) {

    /**
     * Sjekk om saksbehandler har tilgang til å gjøre en gitt handling.
     *
     * @minimumBehandlerRolle den laveste rolle som kreves for den angitte handlingen
     * @handling kort beskrivelse for handlingen. Eksempel: 'endre vilkår', 'oppprette behandling'.
     * Handlingen kommer til saksbehandler så det er viktig at denne gir mening.
     */
    fun harTilgangTilHandling(minimumBehandlerRolle: BehandlerRolle, handling: String) {
        val høyesteRolletilgang = SikkerhetContext.hentHøyesteRolletilgangForInnloggetBruker(rolleConfig)

        if (minimumBehandlerRolle.nivå > høyesteRolletilgang.nivå) {
            throw RolleTilgangskontrollFeil(
                    melding = "${SikkerhetContext.hentSaksbehandlerNavn()} med rolle $høyesteRolletilgang har ikke tilgang til å $handling. Krever $minimumBehandlerRolle.",
                    frontendFeilmelding = "Du har ikke tilgang til å $handling."
            )
        }
    }
}