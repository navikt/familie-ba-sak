package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.steg.StegType

object Behandlingutils {

    fun hentSisteBehandlingSomErIverksatt(iverksatteBehandlinger: List<Behandling>): Behandling? {
        return iverksatteBehandlinger
                .sortedBy { it.opprettetTidspunkt }
                .findLast { !it.erTekniskOpphør() && it.steg == StegType.BEHANDLING_AVSLUTTET }
    }

    fun hentForrigeIverksatteBehandling(iverksatteBehandlinger: List<Behandling>,
                                        behandlingFørFølgende: Behandling): Behandling? {
        return iverksatteBehandlinger
                .filter { it.opprettetTidspunkt.isBefore(behandlingFørFølgende.opprettetTidspunkt) }
                .sortedBy { it.opprettetTidspunkt }
                .findLast { !it.erTekniskOpphør() && it.steg == StegType.BEHANDLING_AVSLUTTET }
    }

    fun bestemUnderkategori(nyBehandling: NyBehandling,
                            aktivBehandlingUnderkategori: BehandlingUnderkategori?): BehandlingUnderkategori {
        return when {
            nyBehandling.underkategori == BehandlingUnderkategori.UTVIDET -> nyBehandling.underkategori

            nyBehandling.behandlingType == BehandlingType.REVURDERING -> aktivBehandlingUnderkategori
                                                                         ?: nyBehandling.underkategori

            else -> nyBehandling.underkategori
        }
    }
}