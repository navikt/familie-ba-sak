package no.nav.familie.ba.sak.journalføring.restDomene

import java.time.LocalDateTime


data class RestOppdaterJournalpost(val avsender: INavnOgIdent,
                                   val bruker: INavnOgIdent,
                                   val datoMottatt: LocalDateTime,
                                   val dokumenttype: String,
                                   val knyttTilFagsak: Boolean,
                                   val eksisterendeLogiskeVedlegg: List<ILogiskVedlegg>,
                                   val logiskeVedlegg: List<ILogiskVedlegg>,
                                   val navIdent: String
)

data class ILogiskVedlegg(val logiskVedleggId: String,
                          val tittel: String)

data class INavnOgIdent (val navn: String,
                         val id: String
)
