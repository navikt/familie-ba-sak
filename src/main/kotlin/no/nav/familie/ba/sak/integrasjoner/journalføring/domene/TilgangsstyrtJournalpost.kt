package no.nav.familie.ba.sak.integrasjoner.journalføring.domene

import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING

data class TilgangsstyrtJournalpost(
    val journalpost: Journalpost,
    val harTilgang: Boolean,
    val adressebeskyttelsegradering: ADRESSEBESKYTTELSEGRADERING?,
)
