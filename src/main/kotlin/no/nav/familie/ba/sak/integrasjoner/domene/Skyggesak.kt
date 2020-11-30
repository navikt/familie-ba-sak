package no.nav.familie.ba.sak.integrasjoner.domene

data class Skyggesak(
        val aktoerId: String,
        val fagsakNr: String,
        val tema: String = "BAR",
        val applikasjon: String = "BA",
)