package no.nav.familie.ba.sak.journalføring.metadata

import org.springframework.stereotype.Component

@Component
object BarnetrygdSøknadMetadata : DokumentMetadata {

    override val journalpostType: JournalpostType = JournalpostType.INNGAAENDE
    override val fagsakSystem: FagsakSystem? = FagsakSystem.BA
    override val tema: String = "BAR"
    override val behandlingstema: String? = "ab0180" // https://confluence.adeo.no/display/BOA/Behandlingstema
    override val kanal: String? = ""
    override val dokumentTypeId: String = "SØKNAD_OM_BARNETRYGD"
    override val tittel: String? = "Søknad om barnetrygd"
    override val brevkode: String? = ""
    override val dokumentKategori: String = "SOK"

}