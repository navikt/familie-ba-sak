package no.nav.familie.ba.sak.dokument.domene

data class DokumentHeaderFelter (
    val fodselsnummer: String,
    val navn: String,
    val adresse: String,
    val postnr: String,
    val returadresse: String,
    val dokumentDato: String
)