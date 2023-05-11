package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus

object BehandlingValidering {

    fun validerBehandlingKanRedigeres(behandling: Behandling) {
        validerBehandlingKanRedigeres(behandling.status)
    }

    fun validerBehandlingKanRedigeres(status: BehandlingStatus) {
        feilHvis(status.erLåstForVidereRedigering()) {
            "Behandlingen er låst for videre redigering"
        }
    }
}
