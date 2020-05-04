package no.nav.familie.ba.sak.journalføring.restDomene

import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg
import java.time.LocalDateTime


data class RestOppdaterJournalpost(val avsender: INavnOgIdent,
                                   val bruker: INavnOgIdent,
                                   val datoMottatt: LocalDateTime,
                                   val dokumenttype: String,
                                   val dokumentInfoId: String,
                                   val knyttTilFagsak: Boolean,
                                   val eksisterendeLogiskeVedlegg: List<LogiskVedlegg>,
                                   val logiskeVedlegg: List<LogiskVedlegg>,
                                   val navIdent: String
)

data class INavnOgIdent (val navn: String,
                         val id: String
)
