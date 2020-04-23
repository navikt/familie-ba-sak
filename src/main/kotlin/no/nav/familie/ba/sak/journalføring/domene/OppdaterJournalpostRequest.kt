package no.nav.familie.ba.sak.journalføring.domene

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.familie.ba.sak.integrasjoner.domene.DokumentInfo
import no.nav.familie.ba.sak.integrasjoner.domene.Sak

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OppdaterJournalpostRequest(val avsender: AvsenderMottaker? = null,
                                      val avsenderMottaker: AvsenderMottaker? = avsender,  // annerledes navngivning i backend, tilpasset både inngående og utgående journalposter
                                      val bruker: Bruker,
                                      val tema: String? = "BAR",
                                      val behandlingstema: String? = null,
                                      val journalfoerendeEnhet: String? = null,
                                      val sak: Sak? = null,
                                      val dokumenter: List<DokumentInfo>? = null,
                                      val dokumentType: String? = null,
                                      val mottattDato: String? = null,
                                      val annentInnhold: String? = null,
                                      val knyttTilFagsak: Boolean)

class Bruker(val idType: IdType? = IdType.FNR,
             val navn: String? = null,
             val id: String)

data class AvsenderMottaker(val id: String,
                            val idType: IdType? = IdType.FNR,
                            val navn: String)

enum class IdType {
    FNR, ORGNR, AKTOERID
}