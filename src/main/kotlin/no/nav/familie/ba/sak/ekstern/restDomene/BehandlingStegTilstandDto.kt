package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.steg.BehandlingStegStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType

class BehandlingStegTilstandDto(
    val behandlingSteg: StegType,
    val behandlingStegStatus: BehandlingStegStatus,
)

fun BehandlingStegTilstand.tilBehandlingStegTilstandDto() =
    BehandlingStegTilstandDto(
        behandlingSteg = this.behandlingSteg,
        behandlingStegStatus = this.behandlingStegStatus,
    )
