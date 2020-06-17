package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg
import java.time.LocalDateTime


data class RestOppdaterJournalpost(val avsender: NavnOgIdent,
                                   val bruker: NavnOgIdent,
                                   val datoMottatt: LocalDateTime?,
                                   val dokumentTittel: String,
                                   val dokumentInfoId: String,
                                   val knyttTilFagsak: Boolean,
                                   val tilknyttedeBehandlingIder: List<String>,
                                   val eksisterendeLogiskeVedlegg: List<LogiskVedlegg>,
                                   val logiskeVedlegg: List<LogiskVedlegg>,
                                   val navIdent: String
)

data class NavnOgIdent(val navn: String,
                       val id: String
)
