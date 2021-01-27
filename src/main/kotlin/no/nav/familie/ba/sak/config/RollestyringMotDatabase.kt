package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Component
import javax.persistence.PrePersist
import javax.persistence.PreRemove
import javax.persistence.PreUpdate


@Component
class RollestyringMotDatabase(
        val rolleConfig: RolleConfig
) {

    @PrePersist
    @PreUpdate
    @PreRemove
    private fun beforeAnyUpdate() {
        val høyesteRolletilgang = SikkerhetContext.hentHøyesteRolletilgangForInnloggetBruker(rolleConfig)

        if (!harSkrivetilgang(høyesteRolletilgang)) {
            throw RolleTilgangskontrollFeil(
                    melding = "${SikkerhetContext.hentSaksbehandlerNavn()} med rolle $høyesteRolletilgang har ikke skrivetilgang til databasen.",
                    frontendFeilmelding = "Du har ikke tilgang til å gjøre denne handlingen."
            )
        }
    }

    private fun harSkrivetilgang(høyesteRolletilgang: BehandlerRolle) =
            høyesteRolletilgang.nivå >= BehandlerRolle.SAKSBEHANDLER.nivå
}
