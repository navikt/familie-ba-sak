package no.nav.familie.ba.sak.kjerne.klage

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import org.springframework.stereotype.Component

@Component
class KlagebehandlingHenter(
    private val klageClient: KlageClient,
) {
    fun hentKlagebehandlingerPåFagsak(fagsakId: Long): List<KlagebehandlingDto> {
        val klagebehandligerPerFagsak = klageClient.hentKlagebehandlinger(setOf(fagsakId))
        val klagerPåFagsak = klagebehandligerPerFagsak[fagsakId]
        if (klagerPåFagsak == null) {
            throw Feil("Fikk ikke fagsakId=$fagsakId tilbake fra kallet til klage.")
        }
        return klagerPåFagsak.map { it.brukVedtaksdatoFraKlageinstansHvisOversendt() }
    }

    fun hentSisteVedtatteKlagebehandling(fagsakId: Long): KlagebehandlingDto? =
        hentKlagebehandlingerPåFagsak(fagsakId)
            .asSequence()
            .filter { SisteVedtatteKlagebehandlingSjekker.harKlagebehandlingKorrektStatus(it.status) }
            .filter { SisteVedtatteKlagebehandlingSjekker.harKlagebehandlingKorrektHenlagtÅrsak(it.henlagtÅrsak) }
            .filter { SisteVedtatteKlagebehandlingSjekker.harKlagebehandlingKorrektBehandlingResultat(it.resultat) }
            .filter { it.vedtaksdato != null }
            .maxByOrNull { it.vedtaksdato!! }

    private object SisteVedtatteKlagebehandlingSjekker {
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
