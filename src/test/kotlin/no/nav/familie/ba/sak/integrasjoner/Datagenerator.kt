package no.nav.familie.ba.sak.integrasjoner

import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.Sakstype
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg
import no.nav.familie.kontrakter.felles.journalpost.RelevantDato
import no.nav.familie.kontrakter.felles.journalpost.Sak
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import java.time.LocalDate
import java.time.LocalDateTime

fun lagTestJournalpost(personIdent: String, journalpostId: String): Journalpost {
    return Journalpost(
            journalpostId = journalpostId,
            journalposttype = Journalposttype.I,
            journalstatus = Journalstatus.MOTTATT,
            tema = Tema.BAR.name,
            behandlingstema = "ab00001",
            bruker = Bruker(personIdent, type = BrukerIdType.FNR),
            journalforendeEnhet = "9999",
            kanal = "NAV_NO",
            dokumenter = listOf(DokumentInfo(tittel = "Søknad om barnetrygd",
                                             brevkode = "mock",
                                             dokumentstatus = null,
                                             dokumentvarianter = emptyList(),
                                             dokumentInfoId = "1",
                                             logiskeVedlegg = listOf(LogiskVedlegg("123", "Oppholdstillatelse"))),
                                DokumentInfo(tittel = "Ekstra vedlegg",
                                             brevkode = "mock",
                                             dokumentstatus = null,
                                             dokumentvarianter = emptyList(),
                                             dokumentInfoId = "2",
                                             logiskeVedlegg = listOf(LogiskVedlegg("123", "Pass")))),
            sak = Sak(arkivsaksnummer = "",
                      arkivsaksystem = "GSAK",
                      sakstype = Sakstype.FAGSAK.name,
                      fagsakId = null,
                      fagsaksystem = FAGSYSTEM),
            tittel = "Søknad om ordinær barnetrygd",
            relevanteDatoer = listOf(RelevantDato(LocalDateTime.now(), "DATO_REGISTRERT"))
    )
}

fun lagTestOppgave(): OpprettOppgaveRequest {
    return OpprettOppgaveRequest(ident = OppgaveIdentV2(ident = "test", gruppe = IdentGruppe.AKTOERID),
                                 saksId = "123",
                                 tema = Tema.BAR,
                                 oppgavetype = Oppgavetype.BehandleSak,
                                 fristFerdigstillelse = LocalDate.now(),
                                 beskrivelse = "test",
                                 enhetsnummer = "1234",
                                 behandlingstema = "behandlingstema")
}

fun lagTestOppgaveDTO(oppgaveId: Long,
                      oppgavetype: Oppgavetype = Oppgavetype.Journalføring,
                      tildeltRessurs: String? = null,
                      tildeltEnhetsnr: String? = "4820"): Oppgave {
    return Oppgave(id = oppgaveId,
                   aktoerId = "1234",
                   identer = listOf(OppgaveIdentV2("11111111111", IdentGruppe.FOLKEREGISTERIDENT)),
                   journalpostId = "1234",
                   tildeltEnhetsnr = tildeltEnhetsnr,
                   tilordnetRessurs = tildeltRessurs,
                   behandlesAvApplikasjon = "FS22",
                   beskrivelse = "Beskrivelse for oppgave",
                   tema = Tema.BAR,
                   oppgavetype = oppgavetype.value,
                   opprettetTidspunkt = LocalDate.of(
                           2020,
                           1,
                           1).toString(),
                   fristFerdigstillelse = LocalDate.of(
                           2020,
                           2,
                           1).toString(),
                   prioritet = OppgavePrioritet.NORM,
                   status = StatusEnum.OPPRETTET
    )
}
