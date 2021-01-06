package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.journalføring.domene.AvsenderMottaker
import no.nav.familie.ba.sak.journalføring.domene.Bruker
import no.nav.familie.ba.sak.journalføring.domene.OppdaterJournalpostRequest
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Dokumentstatus
import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg
import no.nav.familie.kontrakter.felles.journalpost.Sak
import java.time.LocalDateTime

data class RestJournalpostDokument(
        val dokumentTittel: String?,
        val dokumentInfoId: String,
        val brevkode: String?,
        val logiskeVedlegg: List<LogiskVedlegg>?,
        val eksisterendeLogiskeVedlegg: List<LogiskVedlegg>?,
)

data class RestJournalføring(val avsender: NavnOgIdent,
                             val bruker: NavnOgIdent,
                             val datoMottatt: LocalDateTime?,
                             val journalpostTittel: String?,
                             val knyttTilFagsak: Boolean,
                             val opprettOgKnyttTilNyBehandling: Boolean,
                             val tilknyttedeBehandlingIder: List<String>,
                             val dokumenter: List<RestJournalpostDokument>,
                             val navIdent: String
) {

    fun oppdaterMedDokumentOgSak(sak: Sak): OppdaterJournalpostRequest {
        return OppdaterJournalpostRequest(
                avsenderMottaker = AvsenderMottaker(this.avsender.id,
                                                    navn = this.avsender.navn),
                bruker = Bruker(this.bruker.id,
                                navn = this.bruker.navn),
                sak = sak,
                tittel = this.journalpostTittel,
                dokumenter = dokumenter.map { dokument ->
                    DokumentInfo(dokumentInfoId = dokument.dokumentInfoId,
                                 tittel = dokument.dokumentTittel,
                                 brevkode = dokument.brevkode,
                                 dokumentstatus = Dokumentstatus.FERDIGSTILT,
                                 dokumentvarianter = null,
                                 logiskeVedlegg = null)
                },
        )
    }
}