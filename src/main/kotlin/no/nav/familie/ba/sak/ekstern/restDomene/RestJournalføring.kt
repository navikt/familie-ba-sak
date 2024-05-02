package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.erAlfanummeriskPlussKolon
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.Bruker
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.OppdaterJournalpostRequest
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
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

data class RestJournalføring(
    val avsender: NavnOgIdent,
    val bruker: NavnOgIdent,
    val datoMottatt: LocalDateTime?,
    val journalpostTittel: String?,
    val kategori: BehandlingKategori?,
    val underkategori: BehandlingUnderkategori?,
    val knyttTilFagsak: Boolean,
    val opprettOgKnyttTilNyBehandling: Boolean,
    val tilknyttedeBehandlingIder: List<String>,
    val dokumenter: List<RestJournalpostDokument>,
    // Saksbehandler sin ident
    val navIdent: String,
    val nyBehandlingstype: BehandlingType,
    val nyBehandlingsårsak: BehandlingÅrsak,
    val fagsakType: FagsakType,
    val institusjon: RestInstitusjon? = null,
) {
    fun oppdaterMedDokumentOgSak(sak: Sak): OppdaterJournalpostRequest {
        return OppdaterJournalpostRequest(
            avsenderMottaker =
                AvsenderMottaker(
                    id = this.avsender.id,
                    idType = if (this.avsender.id != "") BrukerIdType.FNR else null,
                    navn = this.avsender.navn,
                ),
            bruker =
                Bruker(
                    this.bruker.id,
                    navn = this.bruker.navn,
                ),
            sak = sak,
            tittel = this.journalpostTittel,
            dokumenter =
                dokumenter.map { dokument ->
                    DokumentInfo(
                        dokumentInfoId = dokument.dokumentInfoId,
                        tittel = dokument.dokumentTittel,
                        brevkode = dokument.brevkode,
                        dokumentstatus = Dokumentstatus.FERDIGSTILT,
                        dokumentvarianter = null,
                        logiskeVedlegg = null,
                    )
                },
        )
    }

    fun hentUnderkategori(): BehandlingUnderkategori {
        if (underkategori is BehandlingUnderkategori) return underkategori
        return when {
            journalpostTittel?.contains("ordinær") == true -> BehandlingUnderkategori.ORDINÆR
            journalpostTittel?.contains("utvidet") == true -> BehandlingUnderkategori.UTVIDET
            // Defaulter til ordinær inntil videre.
            else -> BehandlingUnderkategori.ORDINÆR
        }
    }
}

data class NavnOgIdent(
    val navn: String,
    val id: String,
) {
    // Bruker init til å validere personidenten
    init {
        if (!id.erAlfanummeriskPlussKolon()) {
            secureLogger.info("Ugyldig ident: $id")
            throw FunksjonellFeil(
                melding = "Ugyldig ident. Se securelog for mer informasjon.",
                frontendFeilmelding = "Ugyldig ident. Normalt et fødselsnummer eller organisasjonsnummer",
            )
        }
    }
}
