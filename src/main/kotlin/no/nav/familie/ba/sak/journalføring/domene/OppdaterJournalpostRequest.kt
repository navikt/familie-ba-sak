package no.nav.familie.ba.sak.journalføring.domene

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.familie.ba.sak.integrasjoner.domene.DokumentInfo
import no.nav.familie.ba.sak.integrasjoner.domene.Sak

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OppdaterJournalpostRequest(val avsenderMottaker: AvsenderMottaker? = null,
                                      val bruker: Bruker? = null,
                                      val tema: String? = null,
                                      val behandlingstema: String? = null,
                                      val tittel: String? = null,
                                      val journalfoerendeEnhet: String? = null,
                                      val sak: Sak? = null,
                                      val dokumenter: List<DokumentInfo>? = null
)

class Bruker(val idType: IdType? = IdType.FNR,
             val navn: String? = null,
             val id: String)

data class AvsenderMottaker(val id: String,
                            val idType: IdType,
                            val navn: String)

enum class IdType {
    FNR, ORGNR, AKTOERID
}