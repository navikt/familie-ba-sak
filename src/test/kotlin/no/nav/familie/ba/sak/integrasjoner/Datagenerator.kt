package no.nav.familie.ba.sak.integrasjoner

import no.nav.familie.ba.sak.journalføring.domene.Sakstype
import no.nav.familie.ba.sak.oppgave.domene.PrioritetEnum
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.kontrakter.felles.journalpost.*
import no.nav.familie.kontrakter.felles.oppgave.*
import java.time.LocalDate

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
                                             logiskeVedlegg = emptyList())),
            sak = Sak(arkivsaksnummer = "",
                      arkivsaksystem = "GSAK",
                      sakstype = Sakstype.FAGSAK.name,
                      fagsakId = null,
                      fagsaksystem = FAGSYSTEM)
    )
}

fun lagTestOppgave(): OpprettOppgave {
    return OpprettOppgave(ident = OppgaveIdent(ident = "test", type = IdentType.Aktør),
                          saksId = "123",
                          tema = Tema.BAR,
                          oppgavetype = Oppgavetype.BehandleSak,
                          fristFerdigstillelse = LocalDate.now(),
                          beskrivelse = "test",
                          enhetsnummer = "1234",
                          behandlingstema = "behandlingstema")
}

fun lagTestOppgaveDTO(oppgaveId: Long): Oppgave {
    return Oppgave(id = oppgaveId,
                   aktoerId = "1234",
                   journalpostId = "1234",
                   tildeltEnhetsnr = "4820",
                   behandlesAvApplikasjon = "FS22",
                   beskrivelse = "Beskrivelse for oppgave",
                   tema = Tema.BAR,
                   oppgavetype = Oppgavetype.Journalføring.name,
                   opprettetTidspunkt = LocalDate.of(
                           2020,
                           1,
                           1).toString(),
                   fristFerdigstillelse = LocalDate.of(
                           2020,
                           2,
                           1).toString(),
                   prioritet = PrioritetEnum.NORM.name,
                   status = StatusEnum.OPPRETTET
    )
}
