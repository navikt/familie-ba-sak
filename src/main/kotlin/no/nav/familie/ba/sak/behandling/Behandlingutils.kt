package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.steg.StegType

object Behandlingutils {
    fun hentSisteBehandlingSomErIverksatt(behandlinger: List<Behandling>): Behandling? {
        return behandlinger
                .sortedBy { it.opprettetTidspunkt }
                .findLast { it.type != BehandlingType.TEKNISK_OPPHØR && it.steg == StegType.BEHANDLING_AVSLUTTET }
    }

    fun hentForrigeIverksatteBehandling(behandlinger: List<Behandling>, behandlingFørFølgende: Behandling): Behandling? {
        return behandlinger
                .filter { it.opprettetTidspunkt.isBefore(behandlingFørFølgende.opprettetTidspunkt) }
                .sortedBy { it.opprettetTidspunkt }
                .findLast { it.type != BehandlingType.TEKNISK_OPPHØR && it.steg == StegType.BEHANDLING_AVSLUTTET }
    }
}