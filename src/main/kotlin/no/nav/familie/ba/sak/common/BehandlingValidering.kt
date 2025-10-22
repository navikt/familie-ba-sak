package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus

fun validerBehandlingKanRedigeres(behandling: Behandling) {
    validerBehandlingKanRedigeres(behandling.status)
}

fun validerBehandlingKanRedigeres(status: BehandlingStatus) {
    if (status.erLåstForVidereRedigering()) {
        throw FunksjonellFeil(
            melding = "Behandlingen er låst for videre redigering da den har statusen ${status.name}",
        )
    }
}

fun validerBehandlingIkkeErAvsluttet(behandling: Behandling) {
    validerBehandlingIkkeErAvsluttet(behandling.status)
}

fun validerBehandlingIkkeErAvsluttet(status: BehandlingStatus) {
    feilHvis(status == BehandlingStatus.AVSLUTTET) {
        "Behandlingen er avsluttet ($status)"
    }
}
