package no.nav.familie.ba.sak.journalføring.domene

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Journalpost(val journalpostId: String? = null,
                       val journalposttype: Journalposttype? = null,
                       val journalstatus: Journalstatus? = null,
                       val tema: String? = null,
                       val behandlingstema: String? = null,
                       val sak: Sak? = null,
                       val bruker: Bruker? = null,
                       val journalforendeEnhet: String? = null,
                       val kanal: String? = null,
                       val dokumenter: List<DokumentInfo>? = null)

data class Sak(val arkivsaksnummer: String? = null,
               var arkivsaksystem: String? = null,
               val fagsakId: String? = null,
               val fagsaksystem: String? = null)

data class Bruker(val id: String? = null,
                  val type: BrukerIdType? = null)

data class DokumentInfo(val tittel: String? = null,
                        val brevkode: String? = null,
                        val dokumentstatus: Dokumentstatus? = null,
                        val dokumentvarianter: List<Dokumentvariant>? = null)

data class Dokumentvariant(val variantformat: String? = null)

enum class Journalposttype(private val value: String) {
    I("I"),
    U("U"),
    N("N")
}

enum class Journalstatus(private val value: String) {
    MOTTATT("MOTTATT"),
    JOURNALFOERT("JOURNALFOERT"),
    FERDIGSTILT("FERDIGSTILT"),
    EKSPEDERT("EKSPEDERT"),
    UNDER_ARBEID("UNDER_ARBEID"),
    FEILREGISTRERT("FEILREGISTRERT"),
    UTGAAR("UTGAAR"),
    AVBRUTT("AVBRUTT"),
    UKJENT_BRUKER("UKJENT_BRUKER"),
    RESERVERT("RESERVERT"),
    OPPLASTING_DOKUMENT("OPPLASTING_DOKUMENT"),
    UKJENT("UKJENT")
}

enum class Dokumentstatus(private val value: String) {
    FERDIGSTILT("FERDIGSTILT"),
    AVBRUTT("AVBRUTT"),
    UNDER_REDIGERING("UNDER_REDIGERING"),
    KASSERT("KASSERT")
}

enum class BrukerIdType(private val value: String) {
    AKTOERID("AKTOERID"),
    FNR("FNR"),
    ORGNR("ORGNR")
}