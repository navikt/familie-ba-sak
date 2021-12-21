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
            .filter { it.steg == StegType.BEHANDLING_AVSLUTTET }
            .maxByOrNull { it.opprettetTidspunkt }
    }

    fun hentForrigeBehandlingSomErVedtatt(
        behandlinger: List<Behandling>,
        behandlingFørFølgende: Behandling
    ): Behandling? {
        return behandlinger
            .filter { it.opprettetTidspunkt.isBefore(behandlingFørFølgende.opprettetTidspunkt) && it.steg == StegType.BEHANDLING_AVSLUTTET && !it.erHenlagt() }
            .maxByOrNull { it.opprettetTidspunkt }
    }

    fun hentSisteBehandlingSomIkkeErTekniskOpphør(behandlinger: List<Behandling>): Behandling? =
        behandlinger.filter { !it.erTekniskOpphør() }.maxByOrNull { it.opprettetTidspunkt }

    fun hentForrigeIverksatteBehandling(
        iverksatteBehandlinger: List<Behandling>,
        behandlingFørFølgende: Behandling
    ): Behandling? {
        return hentIverksatteBehandlinger(
            iverksatteBehandlinger,
            behandlingFørFølgende
        ).maxByOrNull { it.opprettetTidspunkt }
    }

    fun hentIverksatteBehandlinger(
        iverksatteBehandlinger: List<Behandling>,
        behandlingFørFølgende: Behandling
    ): List<Behandling> {
        return iverksatteBehandlinger
            .filter { it.opprettetTidspunkt.isBefore(behandlingFørFølgende.opprettetTidspunkt) && it.steg == StegType.BEHANDLING_AVSLUTTET }
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
