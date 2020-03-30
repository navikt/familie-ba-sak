package no.nav.familie.integrasjoner.oppgave.domene

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OppgaveDto(val id: Long? = null,
                      val tildeltEnhetsnr: String? = null,
                      val endretAvEnhetsnr: String? = null,
                      val opprettetAvEnhetsnr: String? = null,
                      val journalpostId: String? = null,
                      val journalpostkilde: String? = null,
                      val behandlesAvApplikasjon: String? = null,
                      val saksreferanse: String? = null,
                      val bnr: String? = null,
                      val samhandlernr: String? = null,
                      val aktoerId: String? = null,
                      val orgnr: String? = null,
                      val tilordnetRessurs: String? = null,
                      val beskrivelse: String? = null,
                      val temagruppe: String? = null,
                      val tema: String? = null,
                      val behandlingstema: String? = null,
                      val oppgavetype: String? = null,
                      val behandlingstype: String? = null,
                      val versjon: Int? = null,
                      val mappeId: Long? = null,
                      val fristFerdigstillelse: String? = null,
                      val aktivDato: String? = null,
                      val opprettetTidspunkt: String? = null,
                      val opprettetAv: String? = null,
                      val endretAv: String? = null,
                      val ferdigstiltTidspunkt: String? = null,
                      val endretTidspunkt: String? = null,
                      val prioritet: PrioritetEnum? = null,
                      val status: StatusEnum? = null,
                      private var metadata: MutableMap<String, String>? = null)


enum class StatusEnum(private val value: String) {
    OPPRETTET("OPPRETTET"),
    AAPNET("AAPNET"),
    UNDER_BEHANDLING("UNDER_BEHANDLING"),
    FERDIGSTILT("FERDIGSTILT"),
    FEILREGISTRERT("FEILREGISTRERT");

}

enum class PrioritetEnum(private val value: String) {
    HOY("HOY"),
    NORM("NORM"),
    LAV("LAV");
}
