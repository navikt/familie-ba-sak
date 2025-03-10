package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.common.Feil
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
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Dokumentstatus
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
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
    val nyBehandlingstype: BehandlingType?,
    val nyBehandlingsårsak: BehandlingÅrsak?,
    val fagsakType: FagsakType,
    val institusjon: RestInstitusjon? = null,
) {
    fun valider() {
        if (opprettOgKnyttTilNyBehandling) {
            if (nyBehandlingstype == null) {
                throw FunksjonellFeil("Mangler behandlingstype ved oppretting av ny behandling.")
            }
            if (nyBehandlingsårsak == null && nyBehandlingstype == BehandlingType.REVURDERING) {
                throw FunksjonellFeil("Mangler behandlingsårsak ved oppretting av ny revurdering.")
            }
            if (nyBehandlingsårsak != null && nyBehandlingstype != BehandlingType.REVURDERING) {
                throw FunksjonellFeil("Forventer kun behandlingsårsak ved oppretting av ny revurdering.")
            }
        } else {
            if (nyBehandlingstype != null) {
                throw FunksjonellFeil("Forventet ikke behandlingstype når man ikke skal opprette en ny behandling.")
            }
            if (nyBehandlingsårsak != null) {
                throw FunksjonellFeil("Forventet ikke behandlingsårsak når man ikke skal opprette en ny behandling.")
            }
        }
        if (dokumenter.any { it.dokumentTittel == null || it.dokumentTittel == "" }) {
            throw FunksjonellFeil("Minst ett av dokumentene mangler dokumenttittel.")
        }
    }

    fun oppdaterMedDokumentOgSak(
        sak: Sak,
        journalpost: Journalpost,
    ): OppdaterJournalpostRequest {
        val avsenderMottakerIdType =
            when {
                journalpost.kanal == "EESSI" -> journalpost.avsenderMottaker?.type
                this.avsender.id != "" -> AvsenderMottakerIdType.FNR
                else -> null
            }

        return OppdaterJournalpostRequest(
            avsenderMottaker =
                AvsenderMottaker(
                    id = this.avsender.id,
                    idType = avsenderMottakerIdType,
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

    fun finnBehandlingÅrsakForOpprettingAvNyBehandling(): BehandlingÅrsak {
        if (!opprettOgKnyttTilNyBehandling) {
            throw Feil("Skal ikke kunne opprette ny behandling når 'opprettOgKnyttTilNyBehandling' er false.")
        }
        if (nyBehandlingstype == BehandlingType.REVURDERING) {
            return nyBehandlingsårsak ?: throw Feil("Mangler behandlingsårsak ved oppretting av ny revurdering")
        }
        return BehandlingÅrsak.SØKNAD
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
