package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak

fun validerBrevGenerert(vedtak: Vedtak, rolletilgang: Int) {
    if (vedtak.stønadBrevPdF == null) {
        val feilmelding = "Brev er ikke generert."
        throw if (rolletilgang < BehandlerRolle.SAKSBEHANDLER.nivå) {
            FunksjonellFeil("$feilmelding Veiledere har ikke tilgang til å generere brev.")
        } else Feil(feilmelding)
    }
}
