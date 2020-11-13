package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.steg.StegType

object Behandlingutils {
    fun hentSisteBehandlingSomErIverksatt(iverksatteBehandlinger: List<Behandling>): Behandling? {
        return iverksatteBehandlinger
                .sortedBy { it.opprettetTidspunkt }
                .findLast { !it.erTekniskOpphør() && it.stegTemp == StegType.BEHANDLING_AVSLUTTET }
    }

    fun hentForrigeIverksatteBehandling(iverksatteBehandlinger: List<Behandling>, behandlingFørFølgende: Behandling): Behandling? {
        return iverksatteBehandlinger
                .filter { it.opprettetTidspunkt.isBefore(behandlingFørFølgende.opprettetTidspunkt) }
                .sortedBy { it.opprettetTidspunkt }
                .findLast { !it.erTekniskOpphør() && it.stegTemp == StegType.BEHANDLING_AVSLUTTET }
    }
}