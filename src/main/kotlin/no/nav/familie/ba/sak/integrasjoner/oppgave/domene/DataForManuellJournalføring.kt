package no.nav.familie.ba.sak.integrasjoner.oppgave.domene

import no.nav.familie.ba.sak.ekstern.restDomene.MinimalFagsakDto
import no.nav.familie.ba.sak.ekstern.restDomene.PersonInfoDto
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.oppgave.Oppgave

data class DataForManuellJournalføring(
    val oppgave: Oppgave,
    val person: PersonInfoDto?,
    val journalpost: Journalpost,
    val minimalFagsak: MinimalFagsakDto?,
)
