package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.Journalføringsbehandlingstype
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import java.time.LocalDateTime

data class FerdigstillOppgaveKnyttJournalpostDto(
    val journalpostId: String,
    val tilknyttedeBehandlinger: List<TilknyttetBehandling> = emptyList(),
    val opprettOgKnyttTilNyBehandling: Boolean = false,
    val navIdent: String,
    val bruker: NavnOgIdent,
    val nyBehandlingstype: Journalføringsbehandlingstype,
    val nyBehandlingsårsak: BehandlingÅrsak,
    val kategori: BehandlingKategori?,
    val underkategori: BehandlingUnderkategori?,
    val datoMottatt: LocalDateTime?,
)
