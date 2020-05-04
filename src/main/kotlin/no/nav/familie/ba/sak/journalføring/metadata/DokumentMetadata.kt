package no.nav.familie.ba.sak.journalføring.metadata

interface DokumentMetadata {
    val journalpostType: JournalpostType
    val fagsakSystem: FagsakSystem?
    val tema: String
    val behandlingstema: String?
    val kanal: String?
    val dokumentTypeId: String
    val tittel: String?
    val brevkode: String?
    val dokumentKategori: String
}

enum class JournalpostType {
    INNGAAENDE, UTGAAENDE, NOTAT
}

enum class FagsakSystem {
    BA
}