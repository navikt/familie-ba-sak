package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.steg.StegType

object Behandlingutils {

    fun hentSisteBehandlingSomErIverksatt(iverksatteBehandlinger: List<Behandling>): Behandling? {
        return iverksatteBehandlinger
            .sortedBy { it.opprettetTidspunkt }
            .findLast { !it.erTekniskOpphør() && it.steg == StegType.BEHANDLING_AVSLUTTET }
    }

    fun hentSisteBehandlingSomIkkeErTekniskOpphør(behandlinger: List<Behandling>): Behandling? =
        behandlinger.sortedBy { it.opprettetTidspunkt }.findLast { !it.erTekniskOpphør() }

    fun hentForrigeIverksatteBehandling(
        iverksatteBehandlinger: List<Behandling>,
        behandlingFørFølgende: Behandling
    ): Behandling? {
        return iverksatteBehandlinger
            .filter { it.opprettetTidspunkt.isBefore(behandlingFørFølgende.opprettetTidspunkt) }
            .sortedBy { it.opprettetTidspunkt }
            .findLast { !it.erTekniskOpphør() && it.steg == StegType.BEHANDLING_AVSLUTTET }
    }

    fun bestemKategori(
        behandlingÅrsak: BehandlingÅrsak,
        nyBehandlingKategori: BehandlingKategori?,
        løpendeBehandlingKategori: BehandlingKategori?
    ): BehandlingKategori {
        if (behandlingÅrsak !== BehandlingÅrsak.SØKNAD && løpendeBehandlingKategori != null) {
            return løpendeBehandlingKategori
        }
        return nyBehandlingKategori ?: BehandlingKategori.NASJONAL
    }

    fun bestemUnderkategori(
        nyUnderkategori: BehandlingUnderkategori?,
        nyBehandlingType: BehandlingType,
        løpendeUnderkategori: BehandlingUnderkategori?
    ): BehandlingUnderkategori {
        if (nyUnderkategori == null && løpendeUnderkategori == null) return BehandlingUnderkategori.ORDINÆR
        return when {
            nyUnderkategori == BehandlingUnderkategori.UTVIDET -> nyUnderkategori

            nyBehandlingType == BehandlingType.REVURDERING ->
                løpendeUnderkategori
                    ?: (nyUnderkategori ?: BehandlingUnderkategori.ORDINÆR)

            else -> nyUnderkategori ?: BehandlingUnderkategori.ORDINÆR
        }
    }

    fun utledLøpendeKategori(andeler: List<AndelTilkjentYtelse>): BehandlingKategori {
        return if (andeler.any { it.erEøs() && it.erLøpende() }) BehandlingKategori.EØS else BehandlingKategori.NASJONAL
    }

    fun utledLøpendeUnderkategori(andeler: List<AndelTilkjentYtelse>): BehandlingUnderkategori {
        return if (andeler.any { it.erUtvidet() && it.erLøpende() }) BehandlingUnderkategori.UTVIDET else BehandlingUnderkategori.ORDINÆR
    }
}
