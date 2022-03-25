package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import java.time.LocalDateTime

data class RestLukkOppgaveKnyttJournalpost(
    val knyttJournalpostTilFagsak: Boolean,
    val journalpostId: String?,
    val tilknyttedeBehandlingIder: List<String> = emptyList(),
    val opprettOgKnyttTilNyBehandling: Boolean = false,
    val navIdent: String?,
    val bruker: NavnOgIdent?,
    val nyBehandlingstype: BehandlingType?,
    val nyBehandlingsårsak: BehandlingÅrsak?,
    val kategori: BehandlingKategori?,
    val underkategori: BehandlingUnderkategori?,
    val datoMottatt: LocalDateTime?,
) {
    init {
        if (knyttJournalpostTilFagsak) {
            require(journalpostId != null && journalpostId.isNotEmpty()) { "journalpostId mangler" }

            if (opprettOgKnyttTilNyBehandling) {
                require(navIdent != null && navIdent.isNotEmpty()) { "NavIdent mangler" }
                require(bruker != null) { "bruker mangler" }
                require(nyBehandlingstype != null) { "nyBehandlingstype mangler" }
                require(nyBehandlingsårsak != null) { "nyBehandlingsårsak mangler" }
                require(kategori != null) { "kategori mangler" }
                require(underkategori != null) { "underkategori mangler" }
                require(datoMottatt != null) { "NavIdent mangler" }
            } else {
                require(tilknyttedeBehandlingIder.isNotEmpty()) { "Behandlinger mangler" }
            }
        }
    }
}
