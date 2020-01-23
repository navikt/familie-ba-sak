package no.nav.familie.ba.sak.dokument

data class JournalførBrevTaskDTO(val fnr : String,
                                 val tittel: String,
                                 val brevkode: String,
                                 val behandlingsVedtakId: Long?)
