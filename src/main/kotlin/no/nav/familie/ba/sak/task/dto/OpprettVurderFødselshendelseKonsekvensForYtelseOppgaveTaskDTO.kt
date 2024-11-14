package no.nav.familie.ba.sak.task.dto

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype

typealias AktørId = String

data class OpprettVurderFødselshendelseKonsekvensForYtelseOppgaveTaskDTO(
    val ident: AktørId, // Dette er aktørId (string) og ikke fnr
    val oppgavetype: Oppgavetype,
    val beskrivelse: String,
    val enhetsnummer: String?,
)
