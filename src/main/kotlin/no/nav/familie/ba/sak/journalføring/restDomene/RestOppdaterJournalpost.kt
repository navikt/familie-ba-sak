package no.nav.familie.ba.sak.journalføring.restDomene


data class RestOppdaterJournalpost(val avsender: INavnOgIdent,
                                   val bruker: INavnOgIdent,
                                   val datoMottatt: String,
                                   val dokumenttype: String,
                                   val eksisterendeLogiskeVedlegg: List<ILogiskVedlegg>,
                                   val knyttTilFagsak: Boolean,
                                   val logiskeVedlegg: List<ILogiskVedlegg>,
                                   val navIdent: String
)

data class ILogiskVedlegg(val logiskVedleggId: String,
                          val tittel: String)

data class INavnOgIdent (val navn: String,
                         val id: String
)
