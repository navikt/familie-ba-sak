package no.nav.familie.ba.sak.kjerne.behandling.domene

import java.time.LocalDateTime

data class Visningsbehandling(
    val behandlingId: Long,
    val opprettetTidspunkt: LocalDateTime,
    val aktivertTidspunkt: LocalDateTime,
    val kategori: BehandlingKategori,
    val underkategori: BehandlingUnderkategori,
    val aktiv: Boolean,
    val opprettetÅrsak: BehandlingÅrsak?,
    val type: BehandlingType,
    val status: BehandlingStatus,
    val resultat: Behandlingsresultat,
    val vedtaksdato: LocalDateTime?,
) {
    companion object Factory {
        fun opprettFraBehandling(
            behandling: Behandling,
            vedtaksdato: LocalDateTime?,
        ): Visningsbehandling =
            Visningsbehandling(
                behandlingId = behandling.id,
                opprettetTidspunkt = behandling.opprettetTidspunkt,
                aktivertTidspunkt = behandling.aktivertTidspunkt,
                kategori = behandling.kategori,
                underkategori = behandling.underkategori,
                aktiv = behandling.aktiv,
                opprettetÅrsak = behandling.opprettetÅrsak,
                type = behandling.type,
                status = behandling.status,
                resultat = behandling.resultat,
                vedtaksdato = vedtaksdato,
            )
    }
}
