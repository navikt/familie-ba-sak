package no.nav.familie.ba.sak.kjerne.klage

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import org.springframework.stereotype.Component

@Component
class KlagebehandlingHenter(
    private val klageKlient: KlageKlient,
) {
    fun hentKlagebehandlingerPåFagsak(fagsakId: Long): List<KlagebehandlingDto> {
        val klagerPåFagsak = klageKlient.hentKlagebehandlinger(fagsakId)
        return klagerPåFagsak.map { it.brukVedtaksdatoFraKlageinstansHvisOversendt() }
    }

    fun hentForrigeVedtatteKlagebehandling(behandling: Behandling): KlagebehandlingDto? =
        hentKlagebehandlingerPåFagsak(behandling.fagsak.id)
            .asSequence()
            .filter { ForrigeVedtatteKlagebehandlingSjekker.harKlagebehandlingKorrektStatus(it.status) }
            .filter { ForrigeVedtatteKlagebehandlingSjekker.harKlagebehandlingKorrektHenlagtÅrsak(it.henlagtÅrsak) }
            .filter { ForrigeVedtatteKlagebehandlingSjekker.harKlagebehandlingKorrektBehandlingResultat(it.resultat) }
            .filter { it.vedtaksdato != null }
            .filter { it.vedtaksdato!!.isBefore(behandling.aktivertTidspunkt) }
            .maxByOrNull { it.vedtaksdato!! }

    private object ForrigeVedtatteKlagebehandlingSjekker {
        fun harKlagebehandlingKorrektStatus(behandlingStatus: BehandlingStatus) =
            when (behandlingStatus) {
                BehandlingStatus.FERDIGSTILT,
                -> true

                BehandlingStatus.OPPRETTET,
                BehandlingStatus.UTREDES,
                BehandlingStatus.VENTER,
                BehandlingStatus.SATT_PÅ_VENT,
                -> false
            }

        fun harKlagebehandlingKorrektHenlagtÅrsak(henlagtÅrsak: HenlagtÅrsak?): Boolean {
            if (henlagtÅrsak == null) {
                return true
            }
            return when (henlagtÅrsak) {
                HenlagtÅrsak.TRUKKET_TILBAKE,
                HenlagtÅrsak.FEILREGISTRERT,
                -> false
            }
        }

        fun harKlagebehandlingKorrektBehandlingResultat(behandlingResultat: BehandlingResultat?): Boolean {
            if (behandlingResultat == null) {
                return false
            }
            return when (behandlingResultat) {
                BehandlingResultat.MEDHOLD,
                BehandlingResultat.IKKE_MEDHOLD,
                BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST,
                -> true

                BehandlingResultat.IKKE_SATT,
                BehandlingResultat.HENLAGT,
                -> false
            }
        }
    }
}
