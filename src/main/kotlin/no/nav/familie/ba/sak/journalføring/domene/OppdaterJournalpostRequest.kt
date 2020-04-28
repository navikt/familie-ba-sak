package no.nav.familie.ba.sak.journalføring.domene

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Sak
import java.time.LocalDateTime


// TODO vurdere å fjerne denne og lage en for oppdatering mot familie-integrasjoner og en mellom frontend og backend
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OppdaterJournalpostRequest(val avsender: AvsenderMottaker? = null,
                                      val avsenderMottaker: AvsenderMottaker? = avsender,  // annerledes navngivning i backend, tilpasset både inngående og utgående journalposter
                                      val bruker: Bruker,
                                      val tema: String? = "BAR",
                                      val sak: Sak? = null,
                                      val dokumenter: List<DokumentInfo>? = null,
                                      val dokumentType: String? = null,
                                      val datoMottatt: String,
                                      val logiskeVedlegg: List<String>,
                                      val knyttTilFagsak: Boolean)

data class AvsenderMottaker(val id: String,
                            val idType: IdType? = IdType.FNR,
                            val navn: String)

enum class IdType {
    FNR, ORGNR, AKTOERID
}

enum class Sakstype(val type: String) {
    FAGSAK("FAGSAK"),
    GENERELL_SAK("GENERELL_SAK")
}