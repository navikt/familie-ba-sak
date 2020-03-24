package no.nav.familie.integrasjoner.oppgave.domene

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OppgaveDto(val id: Long?,
                      val tildeltEnhetsnr: String?,
                      val endretAvEnhetsnr: String?,
                      val opprettetAvEnhetsnr: String?,
                      val journalpostId: String?,
                      val journalpostkilde: String?,
                      val behandlesAvApplikasjon: String?,
                      val saksreferanse: String?,
                      val bnr: String?,
                      val samhandlernr: String?,
                      val aktoerId: String?,
                      val orgnr: String?,
                      val tilordnetRessurs: String?,
                      val beskrivelse: String?,
                      val temagruppe: String?,
                      val tema: String?,
                      val behandlingstema: String?,
                      val oppgavetype: String?,
                      val behandlingstype: String?,
                      val versjon: Int?,
                      val mappeId: Long?,
                      val fristFerdigstillelse: String?,
                      val aktivDato: String?,
                      val opprettetTidspunkt: String?,
                      val opprettetAv: String?,
                      val endretAv: String?,
                      val ferdigstiltTidspunkt: String?,
                      val endretTidspunkt: String?,
                      val prioritet: PrioritetEnum?,
                      val status: StatusEnum?,
                      private var metadata: MutableMap<String, String>?)


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
