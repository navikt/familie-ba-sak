package no.nav.familie.ba.sak.dokument.domene

data class DokumentHeaderFelter (
    val fodselsnummer: String,
    val navn: String,
    val returadresse: String,
    val dokumentDato: String
)