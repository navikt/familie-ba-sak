package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.ekstern.restDomene.NavnOgIdent
import no.nav.familie.ba.sak.ekstern.restDomene.RestJournalføring
import no.nav.familie.ba.sak.ekstern.restDomene.RestJournalpostDokument
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.DEFAULT_JOURNALFØRENDE_ENHET
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.Sakstype
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg
import no.nav.familie.kontrakter.felles.journalpost.RelevantDato
import no.nav.familie.kontrakter.felles.journalpost.Sak
import no.nav.familie.kontrakter.felles.journalpost.TilgangsstyrtJournalpost
import java.time.LocalDateTime

fun lagTestJournalpost(
    personIdent: String,
    journalpostId: String,
    avsenderMottakerIdType: AvsenderMottakerIdType?,
    kanal: String,
    sak: Sak? = lagSak(),
): Journalpost =
    Journalpost(
        journalpostId = journalpostId,
        journalposttype = Journalposttype.I,
        journalstatus = Journalstatus.MOTTATT,
        tema = Tema.BAR.name,
        behandlingstema = "ab00001",
        bruker = Bruker(personIdent, type = BrukerIdType.FNR),
        avsenderMottaker =
            AvsenderMottaker(
                navn = "BLÅØYD HEST",
                erLikBruker = true,
                id = personIdent,
                land = "NO",
                type = avsenderMottakerIdType,
            ),
        journalforendeEnhet = DEFAULT_JOURNALFØRENDE_ENHET,
        kanal = kanal,
        dokumenter =
            listOf(
                DokumentInfo(
                    tittel = "Søknad om barnetrygd",
                    brevkode = "NAV 33-00.07",
                    dokumentstatus = null,
                    dokumentvarianter = emptyList(),
                    dokumentInfoId = "1",
                    logiskeVedlegg = listOf(LogiskVedlegg("123", "Oppholdstillatelse")),
                ),
                DokumentInfo(
                    tittel = "Ekstra vedlegg",
                    brevkode = null,
                    dokumentstatus = null,
                    dokumentvarianter = emptyList(),
                    dokumentInfoId = "2",
                    logiskeVedlegg = listOf(LogiskVedlegg("123", "Pass")),
                ),
            ),
        sak = sak,
        tittel = "Søknad om ordinær barnetrygd",
        relevanteDatoer = listOf(RelevantDato(LocalDateTime.now(), "DATO_REGISTRERT")),
    )

private fun lagSak() =
    Sak(
        arkivsaksnummer = "",
        arkivsaksystem = "GSAK",
        sakstype = Sakstype.FAGSAK.name,
        fagsakId = "10695768",
        fagsaksystem = FAGSYSTEM,
    )

fun lagTilgangsstyrtJournalpost(
    personIdent: String,
    journalpostId: String,
    harTilgang: Boolean = true,
): TilgangsstyrtJournalpost =
    TilgangsstyrtJournalpost(
        lagTestJournalpost(
            personIdent = personIdent,
            journalpostId = journalpostId,
            avsenderMottakerIdType = AvsenderMottakerIdType.FNR,
            kanal = "NAV_NO",
        ),
        harTilgang = harTilgang,
    )

fun lagMockRestJournalføring(
    bruker: NavnOgIdent = NavnOgIdent("testbruker", "testIdent"),
    datoMottatt: LocalDateTime = LocalDateTime.now().minusDays(10),
    journalpostTittel: String = "Søknad om ordinær barnetrygd",
    kategori: BehandlingKategori? = BehandlingKategori.NASJONAL,
    underkategori: BehandlingUnderkategori? = BehandlingUnderkategori.ORDINÆR,
    knyttTilFagsak: Boolean = true,
    opprettOgKnyttTilNyBehandling: Boolean = true,
    tilknyttedeBehandlingIder: List<String> = emptyList(),
    dokumenter: List<RestJournalpostDokument> =
        listOf(
            lagRestJournalpostDokument(
                dokumentTittel = "Søknad om barnetrygd",
                brevkode = "mock",
                dokumentInfoId = "1",
                logiskeVedlegg = listOf(LogiskVedlegg("123", "Oppholdstillatelse")),
                eksisterendeLogiskeVedlegg = emptyList(),
            ),
            lagRestJournalpostDokument(
                dokumentTittel = "Ekstra vedlegg",
                brevkode = "mock",
                dokumentInfoId = "2",
                logiskeVedlegg = listOf(LogiskVedlegg("123", "Pass")),
                eksisterendeLogiskeVedlegg = emptyList(),
            ),
        ),
    navIdent: String = "09123",
    fagsakType: FagsakType = FagsakType.NORMAL,
    nyBehandlingstype: BehandlingType? = BehandlingType.FØRSTEGANGSBEHANDLING,
    nyBehandlingsårsak: BehandlingÅrsak? = BehandlingÅrsak.SØKNAD,
): RestJournalføring =
    RestJournalføring(
        avsender = bruker,
        bruker = bruker,
        datoMottatt = datoMottatt,
        journalpostTittel = journalpostTittel,
        kategori = kategori,
        underkategori = underkategori,
        knyttTilFagsak = knyttTilFagsak,
        opprettOgKnyttTilNyBehandling = opprettOgKnyttTilNyBehandling,
        tilknyttedeBehandlingIder = tilknyttedeBehandlingIder,
        dokumenter = dokumenter,
        navIdent = navIdent,
        nyBehandlingstype = nyBehandlingstype,
        nyBehandlingsårsak = nyBehandlingsårsak,
        fagsakType = fagsakType,
    )

fun lagRestJournalpostDokument(
    dokumentTittel: String? = "Søknad om barnetrygd",
    brevkode: String = "mock",
    dokumentInfoId: String? = "1",
    logiskeVedlegg: List<LogiskVedlegg>? = listOf(LogiskVedlegg("123", "Oppholdstillatelse")),
    eksisterendeLogiskeVedlegg: List<LogiskVedlegg>? = emptyList(),
) = RestJournalpostDokument(
    dokumentTittel,
    brevkode,
    dokumentInfoId,
    logiskeVedlegg,
    eksisterendeLogiskeVedlegg,
)
