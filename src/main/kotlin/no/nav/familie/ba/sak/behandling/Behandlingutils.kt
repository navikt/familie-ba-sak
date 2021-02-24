package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelseRepository

object Behandlingutils {

    fun hentIverksatteBehandlinger(behandlinger: List<Behandling>, tilkjentYtelseRepository: TilkjentYtelseRepository): List<Behandling> {
        return behandlinger.filterNot { it.erHenlagt() }
                .filter {
                    tilkjentYtelseRepository.findByBehandlingOptional(it.id)
                            ?.erSendtTilIverksetting() ?: false
                }
    }

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
}