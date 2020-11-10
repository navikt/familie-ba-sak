package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.behandling.steg.BehandlingStegStatus
import no.nav.familie.ba.sak.behandling.steg.StegType

class RestBehandlingStegTilstand (
        val behandlingSteg: StegType,
        val behandlingStegStatus: BehandlingStegStatus
)

fun BehandlingStegTilstand.toRestBehandlingStegTilstand() =
        RestBehandlingStegTilstand(
                behandlingSteg = this.behandlingSteg,
                behandlingStegStatus = this.behandlingStegStatus
        )