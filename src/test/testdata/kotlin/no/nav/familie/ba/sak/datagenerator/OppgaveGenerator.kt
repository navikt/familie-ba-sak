package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import java.time.LocalDate

fun lagTestOppgave(): OpprettOppgaveRequest =
    OpprettOppgaveRequest(
        ident = OppgaveIdentV2(ident = "test", gruppe = IdentGruppe.AKTOERID),
        saksId = "123",
        tema = Tema.BAR,
        oppgavetype = Oppgavetype.BehandleSak,
        fristFerdigstillelse = LocalDate.now(),
        beskrivelse = "test",
        enhetsnummer = "1234",
        behandlingstema = "behandlingstema",
    )

fun lagTestOppgaveDTO(
    oppgaveId: Long,
    oppgavetype: Oppgavetype = Oppgavetype.Journalføring,
    tildeltRessurs: String? = null,
    tildeltEnhetsnr: String? = "4820",
): Oppgave =
    Oppgave(
        id = oppgaveId,
        aktoerId = randomAktør().aktørId,
        identer = listOf(OppgaveIdentV2("11111111111", IdentGruppe.FOLKEREGISTERIDENT)),
        journalpostId = "1234",
        tildeltEnhetsnr = tildeltEnhetsnr,
        tilordnetRessurs = tildeltRessurs,
        behandlesAvApplikasjon = "FS22",
        beskrivelse = "Beskrivelse for oppgave",
        tema = Tema.BAR,
        oppgavetype = oppgavetype.value,
        behandlingstema = Behandlingstema.OrdinærBarnetrygd.value,
        behandlingstype = Behandlingstype.NASJONAL.value,
        opprettetTidspunkt =
            LocalDate
                .of(
                    2020,
                    1,
                    1,
                ).toString(),
        fristFerdigstillelse =
            LocalDate
                .of(
                    2020,
                    2,
                    1,
                ).toString(),
        prioritet = OppgavePrioritet.NORM,
        status = StatusEnum.OPPRETTET,
    )
