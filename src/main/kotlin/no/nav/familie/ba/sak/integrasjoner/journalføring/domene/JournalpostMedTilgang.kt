package no.nav.familie.ba.sak.integrasjoner.journalføring.domene

import no.nav.familie.kontrakter.felles.journalpost.Journalpost

data class JournalpostMedTilgang(
    val journalpost: Journalpost,
    val harTilgang: Boolean,
)
