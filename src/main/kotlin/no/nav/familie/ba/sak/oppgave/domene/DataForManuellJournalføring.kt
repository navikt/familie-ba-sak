package no.nav.familie.ba.sak.oppgave.domene

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.RestPersonInfo
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.oppgave.Oppgave

data class DataForManuellJournalføring(
        val oppgave: Oppgave,
        val person: RestPersonInfo?,
        val journalpost: Journalpost?
)

enum class PrioritetEnum(private val value: String) {
    HOY("HOY"),
    NORM("NORM"),
    LAV("LAV");
}